package com.sl.gateway.filter;

import cn.hutool.crypto.asymmetric.RSA;
import com.itheima.auth.sdk.common.AuthSdkException;
import com.itheima.auth.sdk.dto.AuthUserInfoDTO;
import com.sl.gateway.config.MyConfig;
import com.sl.gateway.properties.JwtProperties;
import com.sl.transport.common.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.security.PublicKey;

/**
 * 用户端token拦截处理
 */
@Slf4j
@Component
public class CustomerTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements AuthFilter {

	@Resource
	private MyConfig myConfig;

	@Resource
	private JwtProperties jwtProperties;

	@Override
	public GatewayFilter apply(Object config) {
		return new TokenGatewayFilter(this.myConfig, this);
	}

	@Override
	public AuthUserInfoDTO check(String token) {
		// 普通用户的token没有对接权限系统，需要自定实现
		try {
			// * Hutool RSA解析
			RSA rsa = new RSA(null, jwtProperties.getPublicKey());
			PublicKey publicKey = rsa.getPublicKey();
			//未配置公钥的情况下本地不做校验
			if (publicKey == null) {
				return null;
			}
			// * 取出userId
			Jws<Claims> jws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
			Long userId = jws.getBody().get(Constants.GATEWAY.USER_ID, Long.class);
			// * 封入token解析对象
			AuthUserInfoDTO authUserInfoDTO = new AuthUserInfoDTO();
			authUserInfoDTO.setUserId(userId);
			return authUserInfoDTO;
		} catch (ExpiredJwtException var3) {
			throw new AuthSdkException("token已过期");
		} catch (Exception var6) {
			log.error("用户端token校验异常：{}", var6.getMessage());
			throw new AuthSdkException("token不合法");
		}
	}

	@Override
	public Boolean auth(String token, AuthUserInfoDTO authUserInfoDTO, String path) {
		//普通用户不需要校验角色
		return true;
	}

	@Override
	public String tokenHeaderName() {
		return Constants.GATEWAY.ACCESS_TOKEN;
	}
}
