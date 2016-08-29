package flavor.pie.countdown;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.service.economy.Currency;

import java.util.Collection;
import java.util.Optional;

import static flavor.pie.countdown.TemporalCurrencies.all;

public class CountdownRegistryModule implements CatalogRegistryModule<Currency> {
    BiMap<String, Currency> map = ImmutableBiMap.copyOf(Maps.toMap(all, Currency::getId)).inverse();
    @Override
    public Optional<Currency> getById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public Collection<Currency> getAll() {
        return all;
    }
}
