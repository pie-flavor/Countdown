package flavor.pie.countdown;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CountdownAccount implements UniqueAccount {
    User user;
    Text name;
    long defaultTicks;
    Countdown plugin;
    CountdownAccount(User user, long defaultTicks, Countdown plugin) {
        this.user = user;
        this.name = Text.of(user.getName());
        this.defaultTicks = defaultTicks;
        this.plugin = plugin;
    }

    @Override
    public Text getDisplayName() {
        return name;
    }

    @Override
    public BigDecimal getDefaultBalance(Currency currency) {
        return BigDecimal.valueOf(((TemporalCurrencies.BaseTemporalCurrency) currency).getTicks());
    }

    @Override
    public boolean hasBalance(Currency currency, Set<Context> contexts) {
        return cast(currency).ticks <= ticks();
    }

    @Override
    public BigDecimal getBalance(Currency currency, Set<Context> contexts) {
        return BigDecimal.valueOf(ticks() / cast(currency).ticks);
    }

    @Override
    public Map<Currency, BigDecimal> getBalances(Set<Context> contexts) {
        ImmutableMap.Builder<Currency, BigDecimal> builder = ImmutableMap.builder();
        long ticks = ticks();
        for (Currency currency : TemporalCurrencies.all) {
            builder.put(currency, BigDecimal.valueOf(ticks / cast(currency).ticks));
        }
        return builder.build();
    }

    @Override
    public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        long ticksToAdd;
        try {
            ticksToAdd = amount.toBigInteger().multiply(cast(currency).ticksI).longValueExact();
        } catch (ArithmeticException ex) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT);
        }
        CountdownData data = data();
        data.ticks = ticksToAdd;
        save(data);
        return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.DEPOSIT);
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(Cause cause, Set<Context> contexts) {
        ImmutableMap.Builder<Currency, TransactionResult> builder = ImmutableMap.builder();
        for (Currency currency : TemporalCurrencies.all) {
            builder.put(currency, new CountdownResult.Transaction(this, currency, BigDecimal.valueOf(defaultTicks / cast(currency).ticks), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW));
        }
        CountdownData data = data();
        data.ticks = defaultTicks;
        save(data);
        return builder.build();
    }

    @Override
    public TransactionResult resetBalance(Currency currency, Cause cause, Set<Context> contexts) {
        return resetBalances(cause, contexts).get(currency);
    }

    @Override
    public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        if (!plugin.spend && data().waitTicks > 0) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT);
        }
        if (amount.signum() != 1) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.DEPOSIT);
        }
        try {
            long ticks = ticks() + amount.toBigInteger().multiply(cast(currency).ticksI).longValueExact();
            if (ticks() < 0) {
                throw new ArithmeticException();
            }
            CountdownData data = data();
            data.ticks = ticks;
            save(data);
        } catch (ArithmeticException ex) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT);
        }
        return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.DEPOSIT);
    }

    @Override
    public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        if (plugin.spend && data().waitTicks > 0) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.WITHDRAW);
        }
        if (amount.signum() != 1) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.WITHDRAW);
        }
        try {
            long ticks = ticks() - (amount.toBigInteger().multiply(cast(currency).ticksI).longValueExact());
            if (ticks < 0 || ticks > ticks()) {
                throw new ArithmeticException();
            }
            CountdownData data = data();
            data.ticks = ticks;
            save(data);
        } catch (ArithmeticException ex) {
            return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.WITHDRAW);
        }
        return new CountdownResult.Transaction(this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW);
    }

    @Override
    public TransferResult transfer(Account to, Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        TransactionResult result = withdraw(currency, amount, cause, contexts);
        if (result.getResult() != ResultType.SUCCESS) {
            return new CountdownResult.Transfer(this, to, currency, amount, contexts, result.getResult(), TransactionTypes.TRANSFER);
        }
        TransactionResult result2 = to.deposit(currency, amount, cause, contexts);
        if (result2.getResult() != ResultType.SUCCESS) {
            deposit(currency, amount, cause, contexts);
            return new CountdownResult.Transfer(this, to, currency, amount, contexts, result2.getResult(), TransactionTypes.TRANSFER);
        }
        return new CountdownResult.Transfer(this, to, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.TRANSFER);
    }

    @Override
    public String getIdentifier() {
        return user.getName();
    }

    @Override
    public Set<Context> getActiveContexts() {
        return ImmutableSet.of();
    }

    @Override
    public UUID getUniqueId() {
        return user.getUniqueId();
    }

    User check(User user) {
        return user.isOnline() ? user.getPlayer().get() : user;
    }

    CountdownData data() {
        return check(user).getOrCreate(CountdownData.class).get();
    }

    long ticks() {
        return data().getTicks();
    }

    TemporalCurrencies.BaseTemporalCurrency cast(Currency currency) {
        return (TemporalCurrencies.BaseTemporalCurrency) currency;
    }
    void save(CountdownData data) {
        check(user).offer(data);
    }
}
