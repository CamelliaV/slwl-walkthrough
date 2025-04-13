package com.sl.ms.carriage.handler;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.EnumUtil;
import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.domain.enums.EconomicRegionEnum;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.transport.common.util.ObjectUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 经济区互寄
 */
@Order(300) //定义顺序
@Component
public class EconomicZoneChainHandler extends AbstractCarriageChainHandler {

	@Resource
	private CarriageTemplateRedisHelper redisHelper;

	@Override
	public CarriageEntity doHandler(WrappedWayBillDTO wrappedWayBillDTO) {
		CarriageEntity carriageEntity = null;

		if (ObjectUtil.isNull(wrappedWayBillDTO.getReceiverProvId())) {
			redisHelper.setProvIdByCityId(wrappedWayBillDTO);
		}

		Map<String, EconomicRegionEnum> EconomicRegionMap = EnumUtil.getEnumMap(EconomicRegionEnum.class);
		EconomicRegionEnum economicRegionEnum = null;
		for (EconomicRegionEnum regionEnum : EconomicRegionMap.values()) {
			// * 经济区是否包含收发件省id
			boolean result = ArrayUtil.containsAll(regionEnum.getValue(), wrappedWayBillDTO.getReceiverProvId(),
					wrappedWayBillDTO.getSenderProvId());
			if (result) {
				economicRegionEnum = regionEnum;
				break;
			}
		}

		// * 属于经济区的情况
		if (ObjectUtil.isNotEmpty(economicRegionEnum)) {
			carriageEntity = redisHelper.getCarriage(CarriageConstant.ECONOMIC_ZONE, CarriageConstant.REGULAR_FAST,
					economicRegionEnum.getCode());
		}

		return doNextHandler(wrappedWayBillDTO, carriageEntity);
	}
}
