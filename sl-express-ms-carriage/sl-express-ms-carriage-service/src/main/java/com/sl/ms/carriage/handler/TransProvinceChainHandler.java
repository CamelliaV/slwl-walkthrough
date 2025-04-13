package com.sl.ms.carriage.handler;

import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.transport.common.util.ObjectUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 跨省
 */
@Order(400) //定义顺序
@Component
public class TransProvinceChainHandler extends AbstractCarriageChainHandler {

	@Resource
	private CarriageTemplateRedisHelper redisHelper;

	@Override
	public CarriageEntity doHandler(WrappedWayBillDTO wrappedWayBillDTO) {
		if (ObjectUtil.isNull(wrappedWayBillDTO.getReceiverProvId())) {
			redisHelper.setProvIdByCityId(wrappedWayBillDTO);
		}
		CarriageEntity carriageEntity = redisHelper.getCarriage(CarriageConstant.TRANS_PROVINCE,
				CarriageConstant.REGULAR_FAST, CarriageConstant.DEFAULT_ASSOCIATED_CITY);
		return doNextHandler(wrappedWayBillDTO, carriageEntity);
	}
}
