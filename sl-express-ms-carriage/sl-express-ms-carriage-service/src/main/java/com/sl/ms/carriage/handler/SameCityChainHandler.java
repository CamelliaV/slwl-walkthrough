package com.sl.ms.carriage.handler;

import cn.hutool.core.util.ObjectUtil;
import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.entity.CarriageEntity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 同城寄
 */
@Order(100) //定义顺序
@Component
public class SameCityChainHandler extends AbstractCarriageChainHandler {

	@Resource
	private CarriageTemplateRedisHelper redisHelper;

	@Override
	public CarriageEntity doHandler(WrappedWayBillDTO wrappedWayBillDTO) {
		CarriageEntity carriageEntity = null;
		WaybillDTO waybillDTO = wrappedWayBillDTO.getWaybillDTO();
		if (ObjectUtil.equals(waybillDTO.getReceiverCityId(), waybillDTO.getSenderCityId())) {
			carriageEntity = redisHelper.getCarriage(CarriageConstant.SAME_CITY, CarriageConstant.REGULAR_FAST,
					CarriageConstant.DEFAULT_ASSOCIATED_CITY);
		}
		return doNextHandler(wrappedWayBillDTO, carriageEntity);
	}
}
