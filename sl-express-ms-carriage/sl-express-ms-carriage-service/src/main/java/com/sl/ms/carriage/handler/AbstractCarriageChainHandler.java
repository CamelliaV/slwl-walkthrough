package com.sl.ms.carriage.handler;

import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.ms.carriage.enums.CarriageExceptionEnum;
import com.sl.transport.common.exception.SLException;

/**
 * 运费模板处理链的抽象定义
 */
public abstract class AbstractCarriageChainHandler {

	private AbstractCarriageChainHandler nextHandler;

	/**
	 * 执行过滤方法，通过输入参数查找运费模板
	 *
	 * @param waybillDTO 输入参数
	 * @return 运费模板
	 */
	public abstract CarriageEntity doHandler(WrappedWayBillDTO waybillDTO);

	/**
	 * 执行下一个处理器
	 *
	 * @param waybillDTO     输入参数
	 * @param carriageEntity 上个handler处理得到的对象
	 * @return
	 */
	protected CarriageEntity doNextHandler(WrappedWayBillDTO waybillDTO, CarriageEntity carriageEntity) {
		if (carriageEntity != null) {
			// * 上个Handler已经找到运费模板就返回
			return carriageEntity;
		}
		if (nextHandler == null) {
			// * 没有更多Handler，抛出未找到异常
			throw new SLException(CarriageExceptionEnum.NOT_FOUND);
		}
		return nextHandler.doHandler(waybillDTO);
	}

	/**
	 * 设置下游Handler
	 *
	 * @param nextHandler 下游Handler
	 */
	public void setNextHandler(AbstractCarriageChainHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
