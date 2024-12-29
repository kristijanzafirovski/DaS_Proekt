import java.time.LocalDate;
import java.time.LocalDateTime;

public class Tick {
    private LocalDate date;
    private double lastTradePrice;
    private double maxPrice;
    private double minPrice;
    private double avgPrice;
    private int volume;

    public Tick(LocalDate date, double lastTradePrice, double maxPrice, double minPrice, double avgPrice, int volume) {
        this.date = date;
        this.lastTradePrice = lastTradePrice;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.avgPrice = avgPrice;
        this.volume = volume;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getLastTradePrice() {
        return lastTradePrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getAvgPrice() {
        return avgPrice;
    }

    public int getVolume() {
        return volume;
    }
}
