package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotTemplateService Unit Tests")
class TimeSlotTemplateServiceTest {

    @Mock
    private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @InjectMocks
    private TimeSlotTemplateService timeSlotTemplateService;

    private Branch testBranch;
    private UserAccount testUser;
    private TimeSlotTemplate testTimeSlot;

    @BeforeEach
    void setUp() {
        // Setup test branch
        testBranch = new Branch();
        testBranch.setId(1L);
        testBranch.setCode("HN01");
        testBranch.setName("Ha Noi Branch");

        // Setup test user with branch access
        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        UserBranches userBranches = new UserBranches();
        userBranches.setBranch(testBranch);
        testUser.setUserBranches(Set.of(userBranches));

        // Setup test time slot
        testTimeSlot = new TimeSlotTemplate();
        testTimeSlot.setId(1L);
        testTimeSlot.setBranch(testBranch);
        testTimeSlot.setName("Morning Slot");
        testTimeSlot.setStartTime(LocalTime.of(9, 0));
        testTimeSlot.setEndTime(LocalTime.of(11, 0));
        testTimeSlot.setStatus(ResourceStatus.ACTIVE);
        testTimeSlot.setCreatedAt(OffsetDateTime.now());
        testTimeSlot.setUpdatedAt(OffsetDateTime.now());
    }

    // ==================== GET ALL TIME SLOTS ====================
    @Nested
    @DisplayName("getAllTimeSlots()")
    class GetAllTimeSlotsTests {

        @Test
        @DisplayName("Trả về danh sách rỗng khi user không có branch access")
        void shouldReturnEmptyListWhenUserHasNoBranchAccess() {
            // Given
            UserAccount userWithNoBranch = new UserAccount();
            userWithNoBranch.setId(2L);
            userWithNoBranch.setUserBranches(Set.of());
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(userWithNoBranch));

            // When
            List<TimeSlotResponseDTO> result = timeSlotTemplateService.getAllTimeSlots(null, null, 2L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Trả về danh sách time slots theo branchId")
        void shouldReturnTimeSlotsByBranchId() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                    .thenReturn(List.of(testTimeSlot));
            when(sessionRepository.countDistinctClassesByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countSessionsByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countFutureSessionsByTimeSlotId(anyLong(), any(), any())).thenReturn(0L);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(anyLong())).thenReturn(false);

            // When
            List<TimeSlotResponseDTO> result = timeSlotTemplateService.getAllTimeSlots(1L, null, 1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Morning Slot");
            assertThat(result.get(0).getBranchId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Ném exception khi user không có quyền truy cập branch")
        void shouldThrowExceptionWhenUserHasNoAccessToBranch() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.getAllTimeSlots(999L, null, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không có quyền truy cập chi nhánh này");
        }

        @Test
        @DisplayName("Lọc time slots theo search keyword")
        void shouldFilterTimeSlotsBySearchKeyword() {
            // Given
            TimeSlotTemplate afternoonSlot = new TimeSlotTemplate();
            afternoonSlot.setId(2L);
            afternoonSlot.setBranch(testBranch);
            afternoonSlot.setName("Afternoon Slot");
            afternoonSlot.setStartTime(LocalTime.of(14, 0));
            afternoonSlot.setEndTime(LocalTime.of(16, 0));
            afternoonSlot.setStatus(ResourceStatus.ACTIVE);

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                    .thenReturn(List.of(testTimeSlot, afternoonSlot));
            when(sessionRepository.countDistinctClassesByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countSessionsByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countFutureSessionsByTimeSlotId(anyLong(), any(), any())).thenReturn(0L);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(anyLong())).thenReturn(false);

            // When
            List<TimeSlotResponseDTO> result = timeSlotTemplateService.getAllTimeSlots(1L, "morning", 1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Morning Slot");
        }
    }

    // ==================== GET TIME SLOT BY ID ====================
    @Nested
    @DisplayName("getTimeSlotById()")
    class GetTimeSlotByIdTests {

        @Test
        @DisplayName("Trả về time slot khi tìm thấy")
        void shouldReturnTimeSlotWhenFound() {
            // Given
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.countDistinctClassesByTimeSlotId(1L)).thenReturn(2L);
            when(sessionRepository.countSessionsByTimeSlotId(1L)).thenReturn(10L);
            when(sessionRepository.countFutureSessionsByTimeSlotId(eq(1L), any(), any())).thenReturn(5L);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(1L)).thenReturn(true);

            // When
            TimeSlotResponseDTO result = timeSlotTemplateService.getTimeSlotById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Morning Slot");
            assertThat(result.getActiveClassesCount()).isEqualTo(2L);
            assertThat(result.getTotalSessionsCount()).isEqualTo(10L);
            assertThat(result.getHasTeacherAvailability()).isTrue();
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi không tìm thấy")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            // Given
            when(timeSlotTemplateRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.getTimeSlotById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Time slot not found");
        }
    }

    // ==================== CREATE TIME SLOT ====================
    @Nested
    @DisplayName("createTimeSlot()")
    class CreateTimeSlotTests {

        @Test
        @DisplayName("Tạo time slot thành công với dữ liệu hợp lệ")
        void shouldCreateTimeSlotSuccessfully() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(1L);
            request.setName("New Slot");
            request.setStartTime("08:00");
            request.setEndTime("10:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(eq(1L), eq("New Slot"), isNull()))
                    .thenReturn(false);
            when(timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(
                    eq(1L), eq(LocalTime.of(8, 0)), eq(LocalTime.of(10, 0)), isNull()))
                    .thenReturn(false);
            when(timeSlotTemplateRepository.save(any(TimeSlotTemplate.class)))
                    .thenAnswer(invocation -> {
                        TimeSlotTemplate saved = invocation.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            // When
            TimeSlotResponseDTO result = timeSlotTemplateService.createTimeSlot(request, 1L, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("New Slot");
            verify(timeSlotTemplateRepository).save(any(TimeSlotTemplate.class));
        }

        @Test
        @DisplayName("Ném exception khi tên trống")
        void shouldThrowExceptionWhenNameIsEmpty() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(1L);
            request.setName("");
            request.setStartTime("08:00");
            request.setEndTime("10:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.createTimeSlot(request, 1L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Vui lòng nhập tên khung giờ");
        }

        @Test
        @DisplayName("Ném exception khi endTime <= startTime")
        void shouldThrowExceptionWhenEndTimeIsNotAfterStartTime() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(1L);
            request.setName("Invalid Slot");
            request.setStartTime("10:00");
            request.setEndTime("08:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.createTimeSlot(request, 1L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Giờ kết thúc phải lớn hơn giờ bắt đầu");
        }

        @Test
        @DisplayName("Ném exception khi tên đã tồn tại")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(1L);
            request.setName("Existing Slot");
            request.setStartTime("08:00");
            request.setEndTime("10:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(eq(1L), eq("Existing Slot"), isNull()))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.createTimeSlot(request, 1L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Tên khung giờ đã tồn tại");
        }

        @Test
        @DisplayName("Ném exception khi khung giờ đã tồn tại")
        void shouldThrowExceptionWhenTimeSlotAlreadyExists() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(1L);
            request.setName("Unique Name");
            request.setStartTime("09:00");
            request.setEndTime("11:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(eq(1L), anyString(), isNull()))
                    .thenReturn(false);
            when(timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(
                    eq(1L), eq(LocalTime.of(9, 0)), eq(LocalTime.of(11, 0)), isNull()))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.createTimeSlot(request, 1L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Khung giờ này đã tồn tại");
        }

        @Test
        @DisplayName("Ném exception khi user không có quyền truy cập branch")
        void shouldThrowExceptionWhenUserHasNoAccessToBranch() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setBranchId(999L);
            request.setName("New Slot");
            request.setStartTime("08:00");
            request.setEndTime("10:00");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.createTimeSlot(request, 1L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Bạn không có quyền truy cập chi nhánh này");
        }
    }

    // ==================== UPDATE TIME SLOT ====================
    @Nested
    @DisplayName("updateTimeSlot()")
    class UpdateTimeSlotTests {

        @Test
        @DisplayName("Cập nhật thành công khi chỉ thay đổi tên")
        void shouldUpdateNameSuccessfully() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setName("Updated Slot Name");

            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(eq(1L), eq("Updated Slot Name"), eq(1L)))
                    .thenReturn(false);
            when(timeSlotTemplateRepository.save(any(TimeSlotTemplate.class))).thenReturn(testTimeSlot);
            when(sessionRepository.countDistinctClassesByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countSessionsByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countFutureSessionsByTimeSlotId(anyLong(), any(), any())).thenReturn(0L);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(anyLong())).thenReturn(false);

            // When
            TimeSlotResponseDTO result = timeSlotTemplateService.updateTimeSlot(1L, request, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(timeSlotTemplateRepository).save(any(TimeSlotTemplate.class));
        }

        @Test
        @DisplayName("Ném exception khi thay đổi thời gian nhưng có session đang dùng")
        void shouldThrowExceptionWhenChangingTimeWithExistingSessions() {
            // Given
            TimeSlotRequestDTO request = new TimeSlotRequestDTO();
            request.setStartTime("10:00");
            request.setEndTime("12:00");

            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.existsByTimeSlotTemplateId(1L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.updateTimeSlot(1L, request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể thay đổi thời gian vì đang được sử dụng");
        }
    }

    // ==================== DELETE TIME SLOT ====================
    @Nested
    @DisplayName("deleteTimeSlot()")
    class DeleteTimeSlotTests {

        @Test
        @DisplayName("Xóa thành công khi time slot INACTIVE và không có liên kết")
        void shouldDeleteSuccessfullyWhenInactiveAndNoRelations() {
            // Given
            testTimeSlot.setStatus(ResourceStatus.INACTIVE);
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.existsByTimeSlotTemplateId(1L)).thenReturn(false);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(1L)).thenReturn(false);

            // When
            timeSlotTemplateService.deleteTimeSlot(1L);

            // Then
            verify(timeSlotTemplateRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Ném exception khi time slot vẫn ACTIVE")
        void shouldThrowExceptionWhenTimeSlotIsActive() {
            // Given
            testTimeSlot.setStatus(ResourceStatus.ACTIVE);
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.deleteTimeSlot(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Vui lòng ngưng hoạt động trước khi xóa");
        }

        @Test
        @DisplayName("Ném exception khi có session đang sử dụng")
        void shouldThrowExceptionWhenSessionsExist() {
            // Given
            testTimeSlot.setStatus(ResourceStatus.INACTIVE);
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.existsByTimeSlotTemplateId(1L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.deleteTimeSlot(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể xóa vì đang được sử dụng");
        }

        @Test
        @DisplayName("Ném exception khi có teacher availability")
        void shouldThrowExceptionWhenTeacherAvailabilityExists() {
            // Given
            testTimeSlot.setStatus(ResourceStatus.INACTIVE);
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.existsByTimeSlotTemplateId(1L)).thenReturn(false);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(1L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.deleteTimeSlot(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể xóa vì đang trong lịch rảnh giáo viên");
        }
    }

    // ==================== UPDATE TIME SLOT STATUS ====================
    @Nested
    @DisplayName("updateTimeSlotStatus()")
    class UpdateTimeSlotStatusTests {

        @Test
        @DisplayName("Kích hoạt time slot thành công")
        void shouldActivateTimeSlotSuccessfully() {
            // Given
            testTimeSlot.setStatus(ResourceStatus.INACTIVE);
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(timeSlotTemplateRepository.save(any(TimeSlotTemplate.class))).thenReturn(testTimeSlot);
            when(sessionRepository.countDistinctClassesByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countSessionsByTimeSlotId(anyLong())).thenReturn(0L);
            when(sessionRepository.countFutureSessionsByTimeSlotId(anyLong(), any(), any())).thenReturn(0L);
            when(teacherAvailabilityRepository.existsById_TimeSlotTemplateId(anyLong())).thenReturn(false);

            // When
            TimeSlotResponseDTO result = timeSlotTemplateService.updateTimeSlotStatus(1L, ResourceStatus.ACTIVE);

            // Then
            assertThat(result).isNotNull();
            verify(timeSlotTemplateRepository).save(argThat(ts -> ts.getStatus() == ResourceStatus.ACTIVE));
        }

        @Test
        @DisplayName("Ném exception khi ngưng hoạt động nhưng có session tương lai")
        void shouldThrowExceptionWhenDeactivatingWithFutureSessions() {
            // Given
            when(timeSlotTemplateRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(sessionRepository.countFutureSessionsByTimeSlotId(eq(1L), any(), any())).thenReturn(5L);

            // When/Then
            assertThatThrownBy(() -> timeSlotTemplateService.updateTimeSlotStatus(1L, ResourceStatus.INACTIVE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể ngưng hoạt động vì có")
                    .hasMessageContaining("lớp học sắp diễn ra");
        }
    }

    // ==================== GET BRANCH TIME SLOT TEMPLATES ====================
    @Nested
    @DisplayName("getBranchTimeSlotTemplates()")
    class GetBranchTimeSlotTemplatesTests {

        @Test
        @DisplayName("Trả về danh sách time slot templates cho dropdown")
        void shouldReturnTimeSlotTemplatesForDropdown() {
            // Given
            TimeSlotTemplate slot1 = new TimeSlotTemplate();
            slot1.setId(1L);
            slot1.setName("Slot 1");
            slot1.setStartTime(LocalTime.of(9, 0));
            slot1.setEndTime(LocalTime.of(11, 0));

            TimeSlotTemplate slot2 = new TimeSlotTemplate();
            slot2.setId(2L);
            slot2.setName("Slot 2");
            slot2.setStartTime(LocalTime.of(14, 0));
            slot2.setEndTime(LocalTime.of(16, 0));

            when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                    .thenReturn(List.of(slot1, slot2));

            // When
            List<TimeSlotTemplateDTO> result = timeSlotTemplateService.getBranchTimeSlotTemplates(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDisplayName()).isEqualTo("09:00 - 11:00");
            assertThat(result.get(1).getDisplayName()).isEqualTo("14:00 - 16:00");
        }
    }
}
