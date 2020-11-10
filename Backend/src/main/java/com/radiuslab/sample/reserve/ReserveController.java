package com.radiuslab.sample.reserve;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.radiuslab.sample.room.RoomRepository;

@RestController
@RequestMapping("/api/reserve")
public class ReserveController {
	@Autowired
	private ReserveService reserveService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ReserveTimeValidator reserveTimeValidator;

	// 예약하기
	@PostMapping
	public ResponseEntity save(@Valid @RequestBody ReserveDto dto, Errors errors) {
		if (errors.hasErrors()) {
			return ResponseEntity.badRequest().body(errors);
		}

		dto.update();
		this.reserveTimeValidator.validate(dto, errors);
		if (errors.hasErrors()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
		}

		if (this.roomRepository.findByRoomId(dto.getRoomId()) == null) {
			errors.rejectValue("roomId", "WrongRoomId", "예약하려는 회의실이 없는 회의실입니다.");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
		}
		List<Reserve> list = this.reserveService.findByReserveDateAndRoomId(dto.getReserveDate().toString(),
				dto.getRoomId());
		for (Reserve r : list) {
			if (dto.getStartTime().isAfter(r.getStartTime()) && dto.getStartTime().isBefore(r.getEndTime())) {
				errors.rejectValue("startTime", "OverlapTime", "다른 예약과 겹치는 시간 입니다.");
			}
			if (dto.getEndTime().isAfter(r.getStartTime()) && dto.getEndTime().isBefore(r.getEndTime())) {
				errors.rejectValue("endTime", "OverlapTime", "다른 예약과 겹치는 시간 입니다.");
			}
			if (errors.hasErrors()) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
			}
		}

		Reserve res = this.reserveService.save(dto);

		URI uri = linkTo(ReserveController.class).slash(res.getReserveId()).toUri();
		return ResponseEntity.created(uri).body(res);
	}

	// 예약수정

	// 예약조회
	@GetMapping("{reserveDate}/all")
	public ResponseEntity<List<Reserve>> findByReserveDate(@PathVariable String reserveDate) {
		List<Reserve> reserveList = this.reserveService.findByReserveDate(reserveDate);
		return new ResponseEntity<List<Reserve>>(reserveList, HttpStatus.OK);
	}

	@GetMapping
	public ResponseEntity<List<Reserve>> checkMonthlyReserve(@RequestParam String roomId, @RequestParam String year,
			@RequestParam String month) {
		List<Reserve> reserveList = this.reserveService.findByRoomIdAndYearMonth(roomId, year, month);
		return new ResponseEntity<List<Reserve>>(reserveList, HttpStatus.OK);
	}

	@GetMapping("{reserveDate}/{roomId}")
	public ResponseEntity<List<Reserve>> findByReserveDateAndRoomId(@PathVariable String reserveDate,
			@PathVariable Long roomId) {
		// if(roomId<0 || roomId > 4) {
		// return ResponseEntity.badRequest().body(error);
		// }
		List<Reserve> reserveList = this.reserveService.findByReserveDateAndRoomId(reserveDate, roomId);
		return new ResponseEntity<List<Reserve>>(reserveList, HttpStatus.OK);
	}

	// 예약취소

	// 비밀번호 확인

}
