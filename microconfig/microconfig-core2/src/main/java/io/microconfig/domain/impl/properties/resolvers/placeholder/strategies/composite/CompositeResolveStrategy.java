package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.composite;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.firstFirstResult;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(PlaceholderResolveStrategy... strategies) {
        return new CompositeResolveStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        return firstFirstResult(strategies, s -> s.resolve(placeholder));
    }
}