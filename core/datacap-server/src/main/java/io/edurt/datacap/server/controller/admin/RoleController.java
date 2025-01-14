package io.edurt.datacap.server.controller.admin;

import io.edurt.datacap.server.body.FilterBody;
import io.edurt.datacap.server.common.Response;
import io.edurt.datacap.server.entity.PageEntity;
import io.edurt.datacap.server.entity.RoleEntity;
import io.edurt.datacap.server.entity.admin.MenuEntity;
import io.edurt.datacap.server.record.TreeRecord;
import io.edurt.datacap.server.repository.RoleRepository;
import io.edurt.datacap.server.service.RoleService;
import io.edurt.datacap.server.validation.ValidationGroup;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/role")
public class RoleController
{
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    public RoleController(RoleRepository roleRepository, RoleService roleService)
    {
        this.roleRepository = roleRepository;
        this.roleService = roleService;
    }

    @PostMapping(value = "list")
    public Response<PageEntity<RoleEntity>> getAllByFilter(@RequestBody FilterBody filter)
    {
        return this.roleService.getAll(roleRepository, filter);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public Response<RoleEntity> save(@RequestBody @Validated(ValidationGroup.Crud.Create.class) RoleEntity configure)
    {
        return this.roleService.saveOrUpdate(roleRepository, configure);
    }

    @GetMapping(value = "{id}")
    public Response<RoleEntity> getInfo(@PathVariable(value = "id") Long id)
    {
        return this.roleService.getById(roleRepository, id);
    }

    @RequestMapping(value = "{id}/menus", method = {RequestMethod.GET, RequestMethod.PUT})
    public Response<? extends Object> getMenusByRoleId(@PathVariable(value = "id") Long id,
            @RequestBody(required = false) List<TreeRecord> nodes)
    {
        if (ObjectUtils.isEmpty(nodes)) {
            return roleService.getMenusByRoleId(id);
        }
        else {
            Optional<RoleEntity> optionalRole = this.roleRepository.findById(id);
            RoleEntity role = optionalRole.get();
            Set<MenuEntity> menus = new HashSet<>();
            extractIds(nodes).forEach(nodeId -> {
                MenuEntity entity = new MenuEntity();
                entity.setId(nodeId);
                menus.add(entity);
            });
            role.setMenus(menus);
            return roleService.saveOrUpdate(roleRepository, role);
        }
    }

    private static Set<Long> extractIds(List<TreeRecord> nodes)
    {
        return nodes.stream()
                .flatMap(node -> {
                    Set<Long> idSet = new HashSet<>();
                    idSet.add(node.getId());
                    List<TreeRecord> childrenList = node.getChildren();
                    if (ObjectUtils.isNotEmpty(childrenList)) {
                        idSet.addAll(extractIds(childrenList));
                    }
                    return idSet.stream();
                })
                .collect(Collectors.toSet());
    }
}
