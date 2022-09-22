package io.edurt.datacap.server.service.impl;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.edurt.datacap.server.adapter.PageRequestAdapter;
import io.edurt.datacap.server.common.Response;
import io.edurt.datacap.server.common.ServiceState;
import io.edurt.datacap.server.entity.PageEntity;
import io.edurt.datacap.server.entity.SourceEntity;
import io.edurt.datacap.server.repository.SourceRepository;
import io.edurt.datacap.server.service.SourceService;
import io.edurt.datacap.spi.FormatType;
import io.edurt.datacap.spi.Plugin;
import io.edurt.datacap.spi.model.Configure;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class SourceServiceImpl
        implements SourceService
{
    private final SourceRepository sourceRepository;
    private final Injector injector;

    public SourceServiceImpl(SourceRepository sourceRepository, Injector injector)
    {
        this.sourceRepository = sourceRepository;
        this.injector = injector;
    }

    @Override
    public Response<SourceEntity> saveOrUpdate(SourceEntity configure)
    {
        return Response.success(this.sourceRepository.save(configure));
    }

    @Override
    public Response<PageEntity<SourceEntity>> getAll(int offset, int limit)
    {
        Pageable pageable = PageRequestAdapter.of(offset, limit);
        return Response.success(PageEntity.build(this.sourceRepository.findAll(pageable)));
    }

    @Override
    public Response<Long> delete(Long id)
    {
        this.sourceRepository.deleteById(id);
        return Response.success(id);
    }

    @Override
    public Response<Object> testConnection(SourceEntity configure)
    {
        Optional<Plugin> pluginOptional = this.injector.getInstance(Key.get(new TypeLiteral<Set<Plugin>>() {}))
                .stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(configure.getType()))
                .findFirst();
        if (!pluginOptional.isPresent()) {
            return Response.failure(ServiceState.PLUGIN_NOT_FOUND);
        }

        Configure _configure = new Configure();
        Plugin plugin = pluginOptional.get();
        _configure.setHost(configure.getHost());
        _configure.setPort(configure.getPort());
        _configure.setUsername(Optional.ofNullable(configure.getUsername()));
        _configure.setPassword(Optional.ofNullable(configure.getPassword()));
        _configure.setEnv(Optional.empty());
        _configure.setFormat(FormatType.JSON);
        plugin.connect(_configure);
        io.edurt.datacap.spi.model.Response response = plugin.execute("SELECT 1");
        plugin.destroy();
        if (response.getIsSuccessful()) {
            return Response.success(response);
        }
        return Response.failure(ServiceState.PLUGIN_EXECUTE_FAILED, response.getMessage());
    }
}
