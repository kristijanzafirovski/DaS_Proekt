package DataScraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class DataScraper {
    public static void main(String[] args) {
        String baseUrl = "https://www.mse.mk/mk/reports";
        String fileBaseUrl = "https://www.mse.mk";
        int currentYear = LocalDate.now().getYear();

        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) { dataDirectory.mkdir(); }

        for (int year = currentYear - 10; year <= currentYear; year++) {
            for (int month = 1; month <= 12; month++) {
                try {
                    // Construct the form data
                    String formData = "cmbMonth=" + month + "&cmbYear=" + year + "&reportCategorySelectList=daily-report";

                    // Send the POST request and get the response
                    Document doc = sendPostRequest(baseUrl, formData);

                    // Select the links to the Excel files
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String fileUrl = link.attr("href");
                        if (fileUrl.endsWith(".xlsx") || fileUrl.endsWith(".xls")) {
                            // Append the base URL if the link is relative
                            if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                                fileUrl = fileBaseUrl + fileUrl;
                                System.out.println(fileUrl);
                            }
                            downloadExcelFile(fileUrl);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Document sendPostRequest(String urlString, String formData) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = formData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStream inputStream = connection.getInputStream();
        return Jsoup.parse(inputStream, "UTF-8", urlString);
    }

    private static void downloadExcelFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String filePath = "data/" + fileName;
        try(InputStream in = new URL(fileUrl).openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(filePath))
        {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println("Downloaded: " + fileUrl);
        } catch (IOException e)
        {
            System.err.println("Failed to download: " + fileUrl);
            e.printStackTrace();
        }
    }
}
