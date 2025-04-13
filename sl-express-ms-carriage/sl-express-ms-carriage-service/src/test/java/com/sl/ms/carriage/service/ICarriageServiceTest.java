package com.sl.ms.carriage.service;

import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.domain.dto.CarriageDTO;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.domain.enums.EconomicRegionEnum;
import com.sl.transport.common.exception.SLException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author CamelliaV
 * @since 2025/2/28 / 17:30
 */
@SpringBootTest
@Slf4j
class ICarriageServiceTest {

	@Resource
	private ICarriageService carriageService;

	@Test
	void saveOrUpdate() {
		CarriageDTO carriageDTO = CarriageDTO.builder()
				.templateType(CarriageConstant.ECONOMIC_ZONE)
				.transportType(CarriageConstant.REGULAR_FAST)
				.associatedCityList(List.of(EconomicRegionEnum.HJL.getCode()))
				.firstWeight(12d)
				.continuousWeight(1d)
				.lightThrowingCoefficient(6000)
				.build();
		assertThrows(SLException.class, () -> {
			carriageService.saveOrUpdate(carriageDTO);
		});
	}


	@Test
	void saveOrUpdateWithNewTemplate() {
		CarriageDTO carriageDTO = CarriageDTO.builder()
				.templateType(CarriageConstant.PROVINCE_ZONE)
				.transportType(CarriageConstant.REGULAR_FAST)
				.associatedCityList(List.of("224649", "161792"))
				.firstWeight(10086d)
				.continuousWeight(0721d)
				.lightThrowingCoefficient(2333)
				.build();
		carriageService.saveOrUpdate(carriageDTO);
	}

	@Test
	void saveOrUpdateWithNewTemplateDeleteRedis() {
		CarriageDTO carriageDTO = CarriageDTO.builder()
				.id(1911340491216035842L)
				.templateType(CarriageConstant.PROVINCE_ZONE)
				.transportType(CarriageConstant.REGULAR_FAST)
				.associatedCityList(List.of("224649", "161792"))
				.firstWeight(114514d)
				.continuousWeight(780d)
				.lightThrowingCoefficient(6969)
				.build();
		carriageService.saveOrUpdate(carriageDTO);
	}

	@Test
	void findAll() {
		List<CarriageDTO> all = carriageService.findAll();
		log.info("All carriages: {}", all);
	}

	@Test
	void compute() {
		WaybillDTO waybillDTO = WaybillDTO.builder()
				.receiverCityId(7363L)
				.senderCityId(2L)
				.weight(3.8)
				.volume(125_000)
				.build();
		CarriageDTO carriageDTO = carriageService.compute(waybillDTO);
		assert carriageDTO != null;
	}

	@Test
	void findByTemplateType() {
	}
}