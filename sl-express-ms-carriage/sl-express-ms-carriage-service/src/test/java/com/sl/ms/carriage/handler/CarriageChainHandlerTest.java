package com.sl.ms.carriage.handler;

import cn.hutool.json.JSONUtil;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.entity.CarriageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class CarriageChainHandlerTest {

	@Resource
	private CarriageChainHandler carriageChainHandler;

	@Test
	void beanToMapTest() {
		CarriageEntity entity = CarriageEntity.builder()
				.templateType(1)
				.transportType(1)
				.associatedCity("1,2")
				.firstWeight(1.0)
				.continuousWeight(2.0)
				.lightThrowingCoefficient(1)
				.build();
		System.out.println(JSONUtil.toJsonStr(entity));
		Map<String, Double> map = new HashMap<>();
		map.put("firstWeight", entity.getFirstWeight());
		map.put("continuousWeight", entity.getContinuousWeight());
		map.put("lightThrowingCoefficient", entity.getLightThrowingCoefficient().doubleValue());
		String jsonStr = JSONUtil.toJsonStr(map);
		System.out.println(jsonStr);
	}

	@Test
	void findCarriage() {
		WaybillDTO waybillDTO = WaybillDTO.builder()
				.senderCityId(161793L)
				.receiverCityId(224650L)
				.volume(1)
				.weight(1d)
				.build();

		CarriageEntity carriage = carriageChainHandler.findCarriage(waybillDTO);
		System.out.println(carriage);
	}
}