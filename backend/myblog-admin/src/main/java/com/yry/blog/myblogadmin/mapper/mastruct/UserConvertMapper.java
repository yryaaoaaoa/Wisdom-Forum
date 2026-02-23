package com.yry.blog.myblogadmin.mapper.mastruct;

import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogadmin.dto.UserRegisterDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogcommon.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConvertMapper {
    @Mapping(target = "id", ignore = true) // @Mapping用于定义对象之间属性映射的规则，在这里是忽略ID
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User registerDtoToUser(UserRegisterDTO dto);

    UserAdminVO userToAdminDTO(User user);

    UserUpdateDTO userToUpdateDTO(User user);  // 自动映射

}
