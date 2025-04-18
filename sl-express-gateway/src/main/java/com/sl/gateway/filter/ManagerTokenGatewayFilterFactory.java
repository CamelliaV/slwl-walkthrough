package com.sl.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.itheima.auth.factory.AuthTemplateFactory;
import com.itheima.auth.sdk.AuthTemplate;
import com.itheima.auth.sdk.common.Result;
import com.itheima.auth.sdk.dto.AuthUserInfoDTO;
import com.itheima.auth.sdk.service.TokenCheckService;
import com.sl.gateway.config.MyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 后台管理员token拦截处理
 */
@Component
public class ManagerTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements AuthFilter {

	@Resource
	private MyConfig myConfig;
	@Resource
	private TokenCheckService tokenCheckService;
	@Value("${role.manager}")
	private List<Long> managerIds;

	@Override
	public GatewayFilter apply(Object config) {
		//由于实现了AuthFilter接口，所以可以传递this对象到TokenGatewayFilter中
		return new TokenGatewayFilter(this.myConfig, this);
	}

	@Override
	public AuthUserInfoDTO check(String token) {
		//校验token
		return tokenCheckService.parserToken(token);
	}

	@Override
	public Boolean auth(String token, AuthUserInfoDTO authUserInfoDTO, String path) {
		AuthTemplate authTemplate = AuthTemplateFactory.get(token);
		// * 获取用户对应角色ids
		Result<List<Long>> roleByUserId = authTemplate.opsForRole().findRoleByUserId(authUserInfoDTO.getUserId());
		List<Long> roleIds = roleByUserId.getData();
		// * 判断id是否有落在配置的管理员id中，对应判断是否有权限
		Collection<Long> intersection = CollUtil.intersection(roleIds, managerIds);
		return CollUtil.isNotEmpty(intersection);
	}
}
