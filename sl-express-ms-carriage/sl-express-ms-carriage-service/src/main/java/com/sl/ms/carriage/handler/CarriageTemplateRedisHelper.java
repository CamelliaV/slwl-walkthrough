package com.sl.ms.carriage.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sl.ms.base.api.common.AreaFeign;
import com.sl.ms.base.domain.base.AreaDto;
import com.sl.ms.carriage.domain.constant.CarriageRedisConstant;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.ms.carriage.service.ICarriageService;
import com.sl.transport.common.exception.SLException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author CamelliaV
 * @since 2025/4/12 / 23:01
 */
@Component
public class CarriageTemplateRedisHelper {

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private AreaFeign areaFeign;
	// * 查库，会有循环引用
	@Resource
	private ICarriageService carriageService;

	public void setProvIdByCityId(WrappedWayBillDTO wrappedWayBillDTO) {

		// * 拆包使用 - 获取cityId
		WaybillDTO waybillDTO = wrappedWayBillDTO.getWaybillDTO();
		Long receiverCityId = waybillDTO.getReceiverCityId();
		Long senderCityId = waybillDTO.getSenderCityId();
		List<Long> cityIds = List.of(receiverCityId, senderCityId);

		// * 单次会话批量执行查询省份Id
		List<String> provIdKeys = new ArrayList<>();
		for (Long cityId : cityIds) {
			String key = StrUtil.format(CarriageRedisConstant.CITY_TO_PROVINCE_KEY, cityId);
			provIdKeys.add(key);
		}
		List<Object> results = stringRedisTemplate.executePipelined(
				new SessionCallback<Object>() {
					@Override
					public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
						ValueOperations<String, String> valueOps = (ValueOperations<String, String>) operations.opsForValue();
						for (String provIdKey : provIdKeys) {
							valueOps.getAndExpire(provIdKey, 1, TimeUnit.HOURS);
						}
						return null;

					}
				});


		// * 获取收寄件地址省份id并写入Redis
		List<Long> provIds = new ArrayList<>();
		for (int i = 0; i < cityIds.size(); i++) {
			if (ObjectUtil.isNull(results.get(i))) {
				AreaDto areaDto = areaFeign.get(cityIds.get(i));
				if (ObjectUtil.isNull(areaDto) || ObjectUtil.isNull(areaDto.getParentId())) {
					throw new SLException("Area feign api调用结果异常 - " + cityIds.get(i));
				}
				Long provinceId = areaDto.getParentId();
				stringRedisTemplate.opsForValue().set(provIdKeys.get(i), provinceId.toString(), 1, TimeUnit.HOURS);
				provIds.add(provinceId);
			} else {
				provIds.add(Long.valueOf(results.get(i).toString()));
			}
		}

		wrappedWayBillDTO.setReceiverProvId(provIds.get(0));
		wrappedWayBillDTO.setSenderProvId(provIds.get(1));
	}

	public CarriageEntity getCarriage(Integer templateType, Integer transportType, String associatedCity) {

		// * 构造key查找对应模版redis缓存
		String key = StrUtil.format(CarriageRedisConstant.CARRIAGE_TEMPLATE_KEY, templateType, transportType,
				associatedCity);
		String result = stringRedisTemplate.opsForValue().getAndExpire(key, 1, TimeUnit.HOURS);
		// * 不存在，需要从数据库导入
		if (ObjectUtil.isNull(result)) {
			String[] split = associatedCity.split(",");
			String reversedAssociatedCity;
			if (split.length != 1) {
				List<Long> cityIdList = Arrays.stream(split).map(Long::valueOf).collect(Collectors.toList());
				cityIdList.sort(Comparator.reverseOrder());
				reversedAssociatedCity = cityIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
			} else {
				reversedAssociatedCity = associatedCity;
			}
			CarriageEntity carriageEntity = carriageService.lambdaQuery()
					.eq(CarriageEntity::getTemplateType, templateType)
					.eq(CarriageEntity::getTransportType, transportType)
					.and(p -> p
							.like(CarriageEntity::getAssociatedCity, associatedCity)
							.or()
							.like(CarriageEntity::getAssociatedCity, reversedAssociatedCity)
					)
					.one();
			if (carriageEntity == null) {
				return null;
			}
			Map<String, Double> redisJson = new HashMap<>();
			// * 只存后续计算会涉及到的属性
			redisJson.put("firstWeight", carriageEntity.getFirstWeight());
			redisJson.put("continuousWeight", carriageEntity.getContinuousWeight());
			redisJson.put("lightThrowingCoefficient", carriageEntity.getLightThrowingCoefficient().doubleValue());

			stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisJson), 1, TimeUnit.HOURS);
			// * 直接返回数据库里的结果
			return carriageEntity;
		}

		// * 转换后直接返回（运费计算只涉及三个属性，前端不展示其他属性）
		CarriageEntity bean = JSONUtil.toBean(result, CarriageEntity.class);
		return bean;

	}


}
