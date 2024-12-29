import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.sql.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.StringJoiner;

public class Analysis {
    private static final String DB_URL = "jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            String getTickersQuery = "SELECT ticker FROM tickers";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(getTickersQuery)) {
                while (rs.next()) {
                    String ticker = rs.getString("ticker");
                    System.out.println("Running analysis for ticker: " + ticker);

                    String dailyResults = runAnalysis(ticker, "daily");
                    String weeklyResults = runAnalysis(ticker, "weekly");
                    String monthlyResults = runAnalysis(ticker, "monthly");

                    System.out.println("Daily results for " + ticker + ":");
                    System.out.println(dailyResults);

                    System.out.println("Weekly results for " + ticker + ":");
                    System.out.println(weeklyResults);

                    System.out.println("Monthly results for " + ticker + ":");
                    System.out.println(monthlyResults);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String runAnalysis(String ticker, String period) {
        System.out.println("Starting runAnalysis for " + ticker + " on period: " + period);
        List<Tick> historicalPrices = SQLiteDB.getHistoricalPrices(ticker);

        System.out.println("Fetched historical prices for " + ticker);

        BarSeries series = new BaseBarSeriesBuilder().withName(ticker).build();
        StringJoiner result = new StringJoiner("\n");
        ZonedDateTime lastEndTime = null;

        for (Tick price : historicalPrices) {
            ZonedDateTime endTime = ZonedDateTime.of(price.getDate(), LocalTime.MIDNIGHT, ZoneId.systemDefault());
            if (lastEndTime == null || endTime.isAfter(lastEndTime)) {
                if (price.getLastTradePrice() != 0 || price.getMaxPrice() != 0 ||
                        price.getMinPrice() != 0 || price.getAvgPrice() != 0 || price.getVolume() != 0) {
                    series.addBar(new BaseBar(Duration.ofDays(1), endTime,
                            DecimalNum.valueOf(price.getLastTradePrice()).getDelegate(),
                            DecimalNum.valueOf(price.getMaxPrice()).getDelegate(),
                            DecimalNum.valueOf(price.getMinPrice()).getDelegate(),
                            DecimalNum.valueOf(price.getAvgPrice()).getDelegate(),
                            DecimalNum.valueOf(price.getVolume()).getDelegate()));

                    lastEndTime = endTime;
                }
            }
        }

        System.out.println("Built series of bars for " + ticker);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Moving Averages
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        EMAIndicator ema12 = new EMAIndicator(closePrice, 12);
        EMAIndicator ema26 = new EMAIndicator(closePrice, 26);
        WMAIndicator wma30 = new WMAIndicator(closePrice, 30);

        // Oscillators
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        StochasticOscillatorKIndicator stochasticK = new StochasticOscillatorKIndicator(series, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        AwesomeOscillatorIndicator ao = new AwesomeOscillatorIndicator(series);
        CCIIndicator cci = new CCIIndicator(series, 20);

        Rule entryRule = new CrossedUpIndicatorRule(sma50, sma200)
                .or(new CrossedUpIndicatorRule(macd, new EMAIndicator(macd, 9)))
                .or(new CrossedUpIndicatorRule(stochasticK, DecimalNum.valueOf(80)))
                .or(new CrossedUpIndicatorRule(rsi, DecimalNum.valueOf(70)))
                .or(new CrossedUpIndicatorRule(ao, DecimalNum.valueOf(0)));

        Rule exitRule = new CrossedDownIndicatorRule(sma50, sma200)
                .or(new CrossedDownIndicatorRule(macd, new EMAIndicator(macd, 9)))
                .or(new CrossedDownIndicatorRule(stochasticK, DecimalNum.valueOf(20)))
                .or(new CrossedDownIndicatorRule(rsi, DecimalNum.valueOf(30)))
                .or(new CrossedDownIndicatorRule(ao, DecimalNum.valueOf(0)));

        int buySignals = 0;
        int sellSignals = 0;

        for (int i = 1; i < series.getBarCount(); i++) {
            boolean buySignal = entryRule.isSatisfied(i);
            boolean sellSignal = exitRule.isSatisfied(i);
            if (buySignal) {
                buySignals++;
                result.add(String.format(
                        "<div class='card'>"+
                                "<div class='card-header buy'>BUY</div>"+
                                "<div class='card-body'>Ticker: %s<br>Date: %s<br>Price: %.2f</div>"+
                                "</div>", ticker, series.getBar(i).getEndTime(), closePrice.getValue(i).doubleValue()));
            }
            if (sellSignal) {
                sellSignals++;
                result.add(String.format(
                        "<div class='card'>"+
                                "<div class='card-header sell'>SELL</div>"+
                                "<div class='card-body'>Ticker: %s<br>Date: %s<br>Price: %.2f</div>"+
                                "</div>", ticker, series.getBar(i).getEndTime(), closePrice.getValue(i).doubleValue()));
            }
        }

        String summary = String.format("<div class='card summary'><div class='card-header'>Summary for %s (%s):</div><div class='card-body'>%d buy signals, %d sell signals</div></div>", ticker, period, buySignals, sellSignals);
        result.add(summary);
        System.out.println(summary);

        return result.toString();
    }


}
