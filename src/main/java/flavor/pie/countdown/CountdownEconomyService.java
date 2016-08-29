package flavor.pie.countdown;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static flavor.pie.countdown.TemporalCurrencies.Minute;
import static flavor.pie.countdown.TemporalCurrencies.all;

public class CountdownEconomyService implements EconomyService {
    long defaultTicks;
    LoadingCache<UUID, UniqueAccount> cache = CacheBuilder.newBuilder().build(new CacheLoader<UUID, UniqueAccount>() {
        public UniqueAccount load(UUID id) {
            return new CountdownAccount(Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(id).orElseThrow(IllegalArgumentException::new), defaultTicks, plugin);
        }
    });
    Countdown plugin;
    CountdownEconomyService(long defaultTicks, Countdown plugin) {
        this.defaultTicks = defaultTicks;
        this.plugin = plugin;
    }

    @Override
    public Currency getDefaultCurrency() {
        return Minute;
    }

    @Override
    public Set<Currency> getCurrencies() {
        return all;
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid).isPresent();
    }

    @Override
    public boolean hasAccount(String identifier) {
        return false;
    }

    @Override
    public Optional<UniqueAccount> getOrCreateAccount(UUID uuid) {
        try {
            return Optional.of(cache.getUnchecked(uuid));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Account> getOrCreateAccount(String identifier) {
        return Optional.empty();
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Account> calculator) {

    }
}
