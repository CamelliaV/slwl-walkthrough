package com.sl.ms.carriage.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sl.ms.base.api.common.AreaFeign;
import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.domain.constant.CarriageRedisConstant;
import com.sl.ms.carriage.domain.dto.CarriageDTO;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.domain.enums.EconomicRegionEnum;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.ms.carriage.enums.CarriageExceptionEnum;
import com.sl.ms.carriage.handler.CarriageChainHandler;
import com.sl.ms.carriage.mapper.CarriageMapper;
import com.sl.ms.carriage.service.ICarriageService;
import com.sl.ms.carriage.utils.CarriageUtils;
import com.sl.transport.common.exception.SLException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CamelliaV
 * @since 2025/2/28 / 17:21
 */

@Slf4j
@Service
public class CarriageServiceImpl extends ServiceImpl<CarriageMapper, CarriageEntity> implements ICarriageService {
	@Resource
	private AreaFeign areaFeign;

	@Resource
	private CarriageChainHandler carriageChainHandler;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public CarriageDTO saveOrUpdate(CarriageDTO carriageDto) {
		// * 校验运费模板是否存在，如果不存在直接插入（查询条件：模板类型  运输类型  如果是修改排除当前id）
		List<CarriageEntity> carriageEntityList = lambdaQuery().eq(CarriageEntity::getTemplateType, carriageDto.getTemplateType())
				.eq(CarriageEntity::getTransportType, carriageDto.getTransportType())
				.ne(ObjectUtil.isNotEmpty(carriageDto.getId()), CarriageEntity::getId, carriageDto.getId())
				.list();

		// * 如果没有重复的模板，可以直接插入或更新操作（DTO转entity 保存成功 entity转DTO）
		if (CollUtil.isEmpty(carriageEntityList)) {
			return saveOrUpdateDTO(carriageDto);
		}

		// * 如果存在重复模板，需要判断此次插入的是否为经济区互寄，非经济区互寄不可以重复
		if (ObjectUtil.notEqual(carriageDto.getTemplateType(), CarriageConstant.ECONOMIC_ZONE)) {
			throw new SLException(CarriageExceptionEnum.NOT_ECONOMIC_ZONE_REPEAT);
		}

		// * 如果是经济区互寄类型，需要进一步判断关联城市是否重复，通过集合取交集判断是否重复
		List<String> associatedCityList = carriageEntityList.stream().map(CarriageEntity::getAssociatedCity)
				.map(associatedCity -> StrUtil.splitToArray(associatedCity, ","))
				.flatMap(Arrays::stream)
				.collect(Collectors.toList());
		Collection<String> intersection = CollUtil.intersection(associatedCityList, carriageDto.getAssociatedCityList());
		if (CollUtil.isNotEmpty(intersection)) {
			throw new SLException(CarriageExceptionEnum.ECONOMIC_ZONE_CITY_REPEAT);
		}

		// * 如果没有重复，可以新增或更新（DTO转Entity 保存成功 entity转DTO）
		return saveOrUpdateDTO(carriageDto);
	}

	@NotNull
	private CarriageDTO saveOrUpdateDTO(CarriageDTO carriageDto) {
		CarriageEntity carriageEntity = CarriageUtils.toEntity(carriageDto);
		// * 插入或更新数据库
		saveOrUpdate(carriageEntity);
		// * 如果传入dto带id，为更新操作，需要删除缓存
		if (ObjectUtil.isNotNull(carriageDto.getId())) {
			// * 构造redis key，关联城市依次以较小的id放在前面
			List<String> associatedCityList = carriageDto.getAssociatedCityList();
			String associatedCity = associatedCityList.stream()
					.map(Long::valueOf)
					.sorted()
					.map(String::valueOf)
					.collect(Collectors.joining(
							","));
			String key = StrUtil.format(CarriageRedisConstant.CARRIAGE_TEMPLATE_KEY, carriageDto.getTemplateType(),
					carriageDto.getTransportType(), associatedCity);
			stringRedisTemplate.delete(key);
		}
		return CarriageUtils.toDTO(carriageEntity);
	}

	@Override
	public List<CarriageDTO> findAll() {
		List<CarriageEntity> list = lambdaQuery()
				.orderByDesc(CarriageEntity::getCreated)
				.list();

		List<CarriageDTO> dtoList = list.stream().map(CarriageUtils::toDTO).collect(Collectors.toList());
		return dtoList;
	}

	@Override
	public CarriageDTO compute(WaybillDTO waybillDTO) {
		// * 1. 根据参数查找运费模板
		// * 职责链模式优化前的方案
		// CarriageEntity carriage = findCarriage(waybillDTO);
		// * 职责链模式优化
		CarriageEntity carriage = carriageChainHandler.findCarriage(waybillDTO);

		// * 2. 计算出实际计费重量，最小重量为1kg
		double computeWeight = getComputeWeight(waybillDTO, carriage);

		// * 3. 计算运费，首重价 + 续重价 * (实重-1)，保留一位小数
		double expense = carriage.getFirstWeight() + (computeWeight - 1) * carriage.getContinuousWeight();
		expense = NumberUtil.round(expense, 1).doubleValue();

		// * 4. 封装对象返回
		CarriageDTO carriageDTO = CarriageUtils.toDTO(carriage);
		carriageDTO.setExpense(expense);
		carriageDTO.setComputeWeight(computeWeight);
		return carriageDTO;
	}

	private double getComputeWeight(WaybillDTO waybillDTO, CarriageEntity carriage) {
		// * 计算体积，如果传入体积不需要计算
		Integer volume = waybillDTO.getVolume();
		if (ObjectUtil.isEmpty(volume)) {
			try {
				// * 长*宽*高计算体积
				volume = waybillDTO.getMeasureLong() * waybillDTO.getMeasureWidth() * waybillDTO.getMeasureHigh();
			} catch (Exception e) {
				// * 计算出错设置体积为0
				volume = 0;
			}
		}
		// * 计算体积重量，体积 / 轻抛系数
		BigDecimal volumeWeight = NumberUtil.div(volume, carriage.getLightThrowingCoefficient(), 1);
		// * 取大值
		double computeWeight = NumberUtil.max(volumeWeight.doubleValue(), NumberUtil.round(waybillDTO.getWeight(), 1).doubleValue());
		// * 计算续重
		// * 不满1kg，按1kg计费
		if (computeWeight <= 1) {
			return 1;
		}
		// * 10kg以下续重以0.1kg计量保留1位小数
		if (computeWeight <= 10) {
			return computeWeight;
		}
		// * 100kg以上四舍五入取整
		// * 108.4kg按照108kg收费
		// * 108.5kg按照109kg收费
		if (computeWeight >= 100) {
			return NumberUtil.round(computeWeight, 0).doubleValue();
		}
		// * 10-100kg续重以0.5kg计量保留1位小数；
		// * 0.5为一个计算单位
		// * 18.8kg按照19收费，
		// * 18.4kg按照18.5kg收费

		// * 先向下取整得到整数
		int integer = NumberUtil.round(computeWeight, 0, RoundingMode.DOWN).intValue();

		// * 原数字与整数相减
		double sub = NumberUtil.sub(computeWeight, integer);
		if (sub == 0) {
			return integer;
		}
		// * 如果小数点后小于等于0.5
		if (sub <= 0.5) {
			// * 在整数上加0.5
			return NumberUtil.add(integer, 0.5);
		}
		// * 否则整数加1
		return NumberUtil.add(integer, 1);
	}

	// * 运费DTO找模版
	private CarriageEntity findCarriage(WaybillDTO waybillDTO) {
		// * 1. 校验是否为同城
		if (ObjectUtil.equals(waybillDTO.getReceiverCityId(), waybillDTO.getSenderCityId())) {
			CarriageEntity carriageEntity = this.findByTemplateType(CarriageConstant.SAME_CITY);
			if (ObjectUtil.isNotEmpty(carriageEntity)) {
				return carriageEntity;
			}
		}

		// * 2. 校验是否为省内
		Long receiverProvinceId = areaFeign.get(waybillDTO.getReceiverCityId()).getParentId();
		Long senderProvinceId = areaFeign.get(waybillDTO.getSenderCityId()).getParentId();
		if (ObjectUtil.equals(receiverProvinceId, senderProvinceId)) {
			CarriageEntity carriageEntity = findByTemplateType(CarriageConstant.SAME_PROVINCE);
			if (ObjectUtil.isNotEmpty(carriageEntity)) {
				return carriageEntity;
			}
		}

		// * 3. 校验是否为经济区互寄
		CarriageEntity carriageEntity = findEconomicCarriage(receiverProvinceId, senderProvinceId);
		if (ObjectUtil.isNotEmpty(carriageEntity)) {
			return carriageEntity;
		}

		// * 4. 校验是否为跨省模板
		carriageEntity = findByTemplateType(CarriageConstant.TRANS_PROVINCE);
		if (ObjectUtil.isNotEmpty(carriageEntity)) {
			return carriageEntity;
		}

		// * 5. 模版未找到，抛异常
		throw new SLException(CarriageExceptionEnum.NOT_FOUND);
	}

	private CarriageEntity findEconomicCarriage(Long receiverProvinceId, Long senderProvinceId) {
		// * 获取经济区城市配置枚举
		Map<String, EconomicRegionEnum> economicRegionMap = EnumUtil.getEnumMap(EconomicRegionEnum.class);
		EconomicRegionEnum economicRegionEnum = null;
		for (EconomicRegionEnum regionEnum : economicRegionMap.values()) {
			// * 该经济区是否全部包含收发件省id
			boolean containsAll = ArrayUtil.containsAll(regionEnum.getValue(), receiverProvinceId, senderProvinceId);
			if (containsAll) {
				economicRegionEnum = regionEnum;
				break;
			}
		}
		// * 没有找到对应的经济区
		if (ObjectUtil.isNull(economicRegionEnum)) {
			return null;
		}
		// * 根据类型编码查询
		CarriageEntity carriageEntity = lambdaQuery()
				.eq(CarriageEntity::getTemplateType, CarriageConstant.ECONOMIC_ZONE)
				.eq(CarriageEntity::getTransportType, CarriageConstant.REGULAR_FAST)
				.like(CarriageEntity::getAssociatedCity, economicRegionEnum.getCode())
				.one();
		return carriageEntity;
	}

	@Override
	public CarriageEntity findByTemplateType(Integer templateType) {
		CarriageEntity carriageEntity = lambdaQuery()
				.eq(CarriageEntity::getTemplateType, templateType)
				.eq(CarriageEntity::getTransportType, CarriageConstant.REGULAR_FAST)
				.one();
		return carriageEntity;
	}
}
