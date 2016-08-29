package flavor.pie.countdown;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.*;
import java.util.List;
import java.util.Set;

public class TemporalCurrencies {
    public static abstract class BaseTemporalCurrency implements Currency {
        Text displayName;
        Text pluralDisplayName;
        Text symbol;
        boolean isDefault;
        String id;
        long ticks;
        BigInteger ticksI;

        BaseTemporalCurrency(Text displayName, Text pluralDisplayName, Text symbol, boolean isDefault, String id, long ticks) {
            this.displayName = displayName;
            this.pluralDisplayName = pluralDisplayName;
            this.symbol = symbol;
            this.isDefault = isDefault;
            this.id = id;
            this.ticks = ticks;
            this.ticksI = BigInteger.valueOf(ticks);
        }

        @Override
        public Text getDisplayName() {
            return displayName;
        }

        @Override
        public Text getPluralDisplayName() {
            return pluralDisplayName;
        }

        @Override
        public Text getSymbol() {
            return symbol;
        }

        @Override
        public Text format(BigDecimal amount, int numFractionDigits) {
            return Text.of(amount.round(new MathContext(2, RoundingMode.HALF_UP)).toPlainString(), symbol);
        }

        @Override
        public int getDefaultFractionDigits() {
            return 0;
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return displayName.toPlain();
        }

        public long getTicks() {
            return ticks;
        }

        public BigInteger getTicksI() {
            return ticksI;
        }
    }
    static {
        ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
        builder.add(Tick = new Tick());
        builder.add(Second = new Second());
        builder.add(Minute = new Minute());
        builder.add(Hour = new Hour());
        builder.add(Day = new Day());
        builder.add(Month = new Month());
        builder.add(Year = new Year());
        all = builder.build();
        TICKS = new TickUnit();
        TICK_OF_SECOND = new TickField();
    }
    public static final Currency Tick;
    private static class Tick extends BaseTemporalCurrency {
        Tick() {
            super(Text.of("Tick"), Text.of("TICKS"), Text.of("t"), false, "countdown:tick", 1);
        }
    }
    public static final Currency Second;
    private static class Second extends BaseTemporalCurrency {
        Second() {
            super(Text.of("Second"), Text.of("Seconds"), Text.of("s"), false, "countdown:second", 20);
        }
    }
    public static final Currency Minute;
    private static class Minute extends BaseTemporalCurrency {
        Minute() {
            super(Text.of("Minute"), Text.of("Minutes"), Text.of("m"), true, "countdown:minute", 20*60);
        }
    }
    public static final Currency Hour;
    private static class Hour extends BaseTemporalCurrency {
        Hour() {
            super(Text.of("Hour"), Text.of("Hours"), Text.of("h"), false, "countdown:hour", 20*60*60);
        }
    }
    public static final Currency Day;
    private static class Day extends BaseTemporalCurrency {
        Day() {
            super(Text.of("Day"), Text.of("Days"), Text.of("D"), false, "countdown:day", 20*60*60*24);
        }
    }
    public static final Currency Month;
    private static class Month extends BaseTemporalCurrency {
        Month() {
            super(Text.of("Month"), Text.of("Months"), Text.of("M"), false, "countdown:month", 20*60*60*24*30);
        }
    }
    public static final Currency Year;
    private static class Year extends BaseTemporalCurrency {
        Year() {
            super(Text.of("Year"), Text.of("Years"), Text.of("Y"), false, "countdown:year", 20*60*60*24*365);
        }
    }
    public static final Set<Currency> all;
    public static final TemporalUnit TICKS;
    private static class TickUnit implements TemporalUnit {

        @Override
        public Duration getDuration() {
            return Duration.ofNanos(50000000L);
        }

        @Override
        public boolean isDurationEstimated() {
            return false;
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return (R) temporal.plus(getDuration().multipliedBy(amount));
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return ChronoUnit.NANOS.between(temporal1Inclusive, temporal2Exclusive) / 50000000L;
        }
    }
    public static final TemporalField TICK_OF_SECOND;
    private static class TickField implements TemporalField {

        @Override
        public TemporalUnit getBaseUnit() {
            return TICKS;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return ChronoUnit.SECONDS;
        }

        @Override
        public ValueRange range() {
            return ValueRange.of(0, 19);
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            return temporal.isSupported(ChronoField.MILLI_OF_SECOND);
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            return range();
        }

        @Override
        public long getFrom(TemporalAccessor temporal) {
            return temporal.get(ChronoField.MILLI_OF_SECOND) % 50;
        }

        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            return ChronoField.MILLI_OF_SECOND.adjustInto(temporal, newValue * 50);
        }
    }

    public static class CurrencyDuration implements TemporalAccessor{
        private static final Temporal BASE_TEMPORAL = LocalDateTime.of(0, 1, 1, 0, 0);
        private final Duration duration;
        private final Temporal temporal;

        public CurrencyDuration(Duration duration) {
            this.duration = duration;
            this.temporal = duration.addTo(BASE_TEMPORAL);
        }

        @Override
        public boolean isSupported(TemporalField field) {
            return temporal.isSupported(field);
        }

        @Override
        public long getLong(TemporalField field) {
            if(!isSupported(field)) throw new UnsupportedTemporalTypeException(field.toString());
            return temporal.getLong(field)-BASE_TEMPORAL.getLong(field);
        }

        public Duration getDuration() {
            return duration;
        }

        public static CurrencyDuration parse(String str) {
            return new CurrencyDuration(Duration.between(BASE_TEMPORAL, dtfIn.parse(str, LocalDateTime::from)));
        }

        public static CurrencyDuration of(long ticks) {
            return new CurrencyDuration(Duration.of(ticks, TICKS));
        }

        @Override
        public String toString() {
            return dtfOut.format(this);
        }

        private static final DateTimeFormatter dtfOut = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NEVER).appendLiteral(':')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral(':')
                .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral(':')
                .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral(':')
                .appendValue(TICK_OF_SECOND, 2)
                .toFormatter();
        
        private static final DateTimeFormatter dtfIn = new DateTimeFormatterBuilder()
                .optionalStart()//tick
                .optionalStart()//second
                .optionalStart()//minute
                .optionalStart()//hour
                .optionalStart()//day
                .optionalStart()//month
                .optionalStart()//year
                .appendValue(ChronoField.YEAR).appendLiteral(':').optionalEnd()
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral(':').optionalEnd()
                .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral(':').optionalEnd()
                .appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(':').optionalEnd()
                .appendValue(ChronoField.MINUTE_OF_HOUR).appendLiteral(':').optionalEnd()
                .appendValue(ChronoField.SECOND_OF_MINUTE).appendLiteral(':').optionalEnd()
                .appendValue(TICK_OF_SECOND).appendLiteral(':').optionalEnd()
                .toFormatter();

    }
    public static CommandElement currencyDuration(Text key) {
        return new CurrencyDurationElement(key);
    }
    private static class CurrencyDurationElement extends CommandElement {

        protected CurrencyDurationElement(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            String arg = args.next();
            try {
                return CurrencyDuration.parse(arg);
            } catch (DateTimeParseException ex) {
                throw new ArgumentParseException(Text.of("Could not parse temporal currency amount!"), ex, arg, ex.getErrorIndex());
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return ImmutableList.of();
        }
    }
}
