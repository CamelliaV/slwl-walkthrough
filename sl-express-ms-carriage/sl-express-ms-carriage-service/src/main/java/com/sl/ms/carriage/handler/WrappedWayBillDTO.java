package com.sl.ms.carriage.handler;

import com.sl.ms.carriage.domain.dto.WaybillDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author CamelliaV
 * @since 2025/4/13 / 0:14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrappedWayBillDTO {
	private WaybillDTO waybillDTO;
	private Long receiverProvId;
	private Long senderProvId;
}
