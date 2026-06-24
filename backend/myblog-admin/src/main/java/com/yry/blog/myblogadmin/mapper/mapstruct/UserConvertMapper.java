package com.yry.blog.myblogadmin.mapper.mapstruct;

import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogadmin.dto.UserRegisterDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogcommon.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConvertMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User registerDtoToUser(UserRegisterDTO dto);

    UserAdminVO userToAdminDTO(User user);

    UserUpdateDTO userToUpdateDTO(User user);
}
