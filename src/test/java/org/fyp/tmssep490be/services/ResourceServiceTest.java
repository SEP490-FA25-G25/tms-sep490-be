package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
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
@DisplayName("ResourceService Unit Tests")
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private SessionResourceRepository sessionResourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Branch testBranch;
    private UserAccount testUser;
    private Resource testRoom;
    private Resource testVirtualResource;

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

        // Setup test room resource
        testRoom = new Resource();
        testRoom.setId(1L);
        testRoom.setBranch(testBranch);
        testRoom.setResourceType(ResourceType.ROOM);
        testRoom.setCode("HN01-R101");
        testRoom.setName("Room 101");
        testRoom.setCapacity(20);
        testRoom.setStatus(ResourceStatus.ACTIVE);
        testRoom.setCreatedAt(OffsetDateTime.now());
        testRoom.setUpdatedAt(OffsetDateTime.now());

        // Setup test virtual resource
        testVirtualResource = new Resource();
        testVirtualResource.setId(2L);
        testVirtualResource.setBranch(testBranch);
        testVirtualResource.setResourceType(ResourceType.VIRTUAL);
        testVirtualResource.setCode("HN01-Z01");
        testVirtualResource.setName("Zoom Room 01");
        testVirtualResource.setCapacity(100);
        testVirtualResource.setMeetingUrl("https://zoom.us/j/123456");
        testVirtualResource.setAccountEmail("zoom@company.com");
        testVirtualResource.setStatus(ResourceStatus.ACTIVE);
        testVirtualResource.setCreatedAt(OffsetDateTime.now());
        testVirtualResource.setUpdatedAt(OffsetDateTime.now());
    }

    // ==================== GET ALL RESOURCES ====================
    @Nested
    @DisplayName("getAllResources()")
    class GetAllResourcesTests {

        @Test
        @DisplayName("Trả về danh sách rỗng khi user không có branch access")
        void shouldReturnEmptyListWhenUserHasNoBranchAccess() {
            // Given
            UserAccount userWithNoBranch = new UserAccount();
            userWithNoBranch.setId(2L);
            userWithNoBranch.setUserBranches(Set.of());
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(userWithNoBranch));

            // When
            List<ResourceDTO> result = resourceService.getAllResources(null, null, null, 2L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Trả về danh sách resources theo branchId")
        void shouldReturnResourcesByBranchId() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(resourceRepository.findByBranchIdOrderByNameAsc(1L))
                    .thenReturn(List.of(testRoom, testVirtualResource));
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            List<ResourceDTO> result = resourceService.getAllResources(1L, null, null, 1L);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Lọc resources theo type = ROOM")
        void shouldFilterResourcesByTypeRoom() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(resourceRepository.findByBranchIdOrderByNameAsc(1L))
                    .thenReturn(List.of(testRoom, testVirtualResource));
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            List<ResourceDTO> result = resourceService.getAllResources(1L, "ROOM", null, 1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResourceType()).isEqualTo("ROOM");
        }

        @Test
        @DisplayName("Lọc resources theo search keyword")
        void shouldFilterResourcesBySearchKeyword() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(resourceRepository.findByBranchIdOrderByNameAsc(1L))
                    .thenReturn(List.of(testRoom, testVirtualResource));
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            List<ResourceDTO> result = resourceService.getAllResources(1L, null, "Zoom", 1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).contains("Zoom");
        }

        @Test
        @DisplayName("Ném exception khi user không có quyền truy cập branch")
        void shouldThrowExceptionWhenUserHasNoAccessToBranch() {
            // Given
            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> resourceService.getAllResources(999L, null, null, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không có quyền truy cập chi nhánh này");
        }
    }

    // ==================== GET RESOURCE BY ID ====================
    @Nested
    @DisplayName("getResourceById()")
    class GetResourceByIdTests {

        @Test
        @DisplayName("Trả về resource khi tìm thấy")
        void shouldReturnResourceWhenFound() {
            // Given
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(sessionResourceRepository.countDistinctClassesByResourceId(1L)).thenReturn(2L);
            when(sessionResourceRepository.countSessionsByResourceId(1L)).thenReturn(10L);
            when(sessionResourceRepository.findNextSessionByResourceId(eq(1L), any(), any())).thenReturn(null);

            // When
            ResourceDTO result = resourceService.getResourceById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Room 101");
            assertThat(result.getActiveClassesCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi không tìm thấy")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            // Given
            when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> resourceService.getResourceById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resource not found");
        }
    }

    // ==================== CREATE RESOURCE ====================
    @Nested
    @DisplayName("createResource()")
    class CreateResourceTests {

        @Test
        @DisplayName("Tạo ROOM resource thành công với dữ liệu hợp lệ")
        void shouldCreateRoomResourceSuccessfully() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("ROOM");
            request.setCode("R102");
            request.setName("Room 102");
            request.setCapacity(25);

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(eq(1L), eq("HN01-R102")))
                    .thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(eq(1L), eq("Room 102")))
                    .thenReturn(false);
            when(resourceRepository.save(any(Resource.class)))
                    .thenAnswer(invocation -> {
                        Resource saved = invocation.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            ResourceDTO result = resourceService.createResource(request, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCode()).isEqualTo("HN01-R102");
            verify(resourceRepository).save(any(Resource.class));
        }

        @Test
        @DisplayName("Tạo VIRTUAL resource thành công với Meeting URL")
        void shouldCreateVirtualResourceWithMeetingUrl() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(100);
            request.setMeetingUrl("https://zoom.us/j/654321");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.save(any(Resource.class)))
                    .thenAnswer(invocation -> {
                        Resource saved = invocation.getArgument(0);
                        saved.setId(101L);
                        return saved;
                    });
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            ResourceDTO result = resourceService.createResource(request, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceType()).isEqualTo("VIRTUAL");
        }

        @Test
        @DisplayName("Ném exception khi code trống")
        void shouldThrowExceptionWhenCodeIsEmpty() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("ROOM");
            request.setCode("");
            request.setName("Room 102");
            request.setCapacity(25);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Mã tài nguyên là bắt buộc");
        }

        @Test
        @DisplayName("Ném exception khi name trống")
        void shouldThrowExceptionWhenNameIsEmpty() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("ROOM");
            request.setCode("R102");
            request.setName("");
            request.setCapacity(25);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Tên tài nguyên là bắt buộc");
        }

        @Test
        @DisplayName("Ném exception khi capacity vượt quá giới hạn ROOM (40)")
        void shouldThrowExceptionWhenRoomCapacityExceedsLimit() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("ROOM");
            request.setCode("R102");
            request.setName("Room 102");
            request.setCapacity(50);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Sức chứa của phòng học tối đa là 40 người");
        }

        @Test
        @DisplayName("Ném exception khi capacity vượt quá giới hạn VIRTUAL (100)")
        void shouldThrowExceptionWhenVirtualCapacityExceedsLimit() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(150);
            request.setMeetingUrl("https://zoom.us/j/123");

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Sức chứa của phòng ảo (Zoom) tối đa là 100 người");
        }

        @Test
        @DisplayName("Ném exception khi code đã tồn tại")
        void shouldThrowExceptionWhenCodeAlreadyExists() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("ROOM");
            request.setCode("R101");
            request.setName("Room 101 New");
            request.setCapacity(20);

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(eq(1L), eq("HN01-R101")))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Mã tài nguyên")
                    .hasMessageContaining("đã tồn tại");
        }

        @Test
        @DisplayName("Ném exception khi VIRTUAL resource không có Meeting URL hoặc Account Email")
        void shouldThrowExceptionWhenVirtualResourceMissingRequiredFields() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(50);
            // Missing meetingUrl and accountEmail

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Tài nguyên ảo cần có Meeting URL hoặc Account Email");
        }

        @Test
        @DisplayName("Ném exception khi Meeting URL không hợp lệ")
        void shouldThrowExceptionWhenMeetingUrlIsInvalid() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(50);
            request.setMeetingUrl("invalid-url");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Meeting URL phải bắt đầu bằng http:// hoặc https://");
        }

        @Test
        @DisplayName("Ném exception khi Account Email không hợp lệ")
        void shouldThrowExceptionWhenAccountEmailIsInvalid() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(50);
            request.setAccountEmail("invalid-email");

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Account Email không đúng định dạng email");
        }

        @Test
        @DisplayName("Ném exception khi Expiry Date là ngày trong quá khứ")
        void shouldThrowExceptionWhenExpiryDateIsInPast() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setBranchId(1L);
            request.setResourceType("VIRTUAL");
            request.setCode("Z02");
            request.setName("Zoom Room 02");
            request.setCapacity(50);
            request.setMeetingUrl("https://zoom.us/j/123");
            request.setExpiryDate(LocalDate.now().minusDays(1).toString());

            when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
            when(resourceRepository.existsByBranchIdAndCodeIgnoreCase(anyLong(), anyString())).thenReturn(false);
            when(resourceRepository.existsByBranchIdAndNameIgnoreCase(anyLong(), anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> resourceService.createResource(request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Ngày hết hạn phải là ngày trong tương lai");
        }
    }

    // ==================== UPDATE RESOURCE ====================
    @Nested
    @DisplayName("updateResource()")
    class UpdateResourceTests {

        @Test
        @DisplayName("Cập nhật thành công khi chỉ thay đổi tên")
        void shouldUpdateNameSuccessfully() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setName("Updated Room Name");

            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(resourceRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(eq(1L), eq("Updated Room Name"), eq(1L)))
                    .thenReturn(false);
            when(resourceRepository.save(any(Resource.class))).thenReturn(testRoom);
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            ResourceDTO result = resourceService.updateResource(1L, request, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(resourceRepository).save(any(Resource.class));
        }

        @Test
        @DisplayName("Ném exception khi giảm capacity dưới mức đang sử dụng")
        void shouldThrowExceptionWhenReducingCapacityBelowUsage() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setCapacity(10);

            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(sessionResourceRepository.findMaxClassCapacityByResourceId(1L)).thenReturn(15);

            // When/Then
            assertThatThrownBy(() -> resourceService.updateResource(1L, request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể giảm sức chứa");
        }

        @Test
        @DisplayName("Ném exception khi mô tả < 10 ký tự")
        void shouldThrowExceptionWhenDescriptionTooShort() {
            // Given
            ResourceRequestDTO request = new ResourceRequestDTO();
            request.setDescription("Short");

            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // When/Then
            assertThatThrownBy(() -> resourceService.updateResource(1L, request, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Mô tả phải có ít nhất 10 ký tự");
        }
    }

    // ==================== DELETE RESOURCE ====================
    @Nested
    @DisplayName("deleteResource()")
    class DeleteResourceTests {

        @Test
        @DisplayName("Xóa thành công khi resource INACTIVE và không có session")
        void shouldDeleteSuccessfullyWhenInactiveAndNoSessions() {
            // Given
            testRoom.setStatus(ResourceStatus.INACTIVE);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(sessionResourceRepository.existsByResourceId(1L)).thenReturn(false);

            // When
            resourceService.deleteResource(1L);

            // Then
            verify(resourceRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Ném exception khi resource vẫn ACTIVE")
        void shouldThrowExceptionWhenResourceIsActive() {
            // Given
            testRoom.setStatus(ResourceStatus.ACTIVE);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // When/Then
            assertThatThrownBy(() -> resourceService.deleteResource(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Vui lòng ngưng hoạt động tài nguyên trước khi xóa");
        }

        @Test
        @DisplayName("Ném exception khi có session đang sử dụng")
        void shouldThrowExceptionWhenSessionsExist() {
            // Given
            testRoom.setStatus(ResourceStatus.INACTIVE);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(sessionResourceRepository.existsByResourceId(1L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> resourceService.deleteResource(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể xóa vì tài nguyên này đang được sử dụng");
        }
    }

    // ==================== UPDATE RESOURCE STATUS ====================
    @Nested
    @DisplayName("updateResourceStatus()")
    class UpdateResourceStatusTests {

        @Test
        @DisplayName("Kích hoạt resource thành công")
        void shouldActivateResourceSuccessfully() {
            // Given
            testRoom.setStatus(ResourceStatus.INACTIVE);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(resourceRepository.save(any(Resource.class))).thenReturn(testRoom);
            when(sessionResourceRepository.countDistinctClassesByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.countSessionsByResourceId(anyLong())).thenReturn(0L);
            when(sessionResourceRepository.findNextSessionByResourceId(anyLong(), any(), any())).thenReturn(null);

            // When
            ResourceDTO result = resourceService.updateResourceStatus(1L, ResourceStatus.ACTIVE);

            // Then
            assertThat(result).isNotNull();
            verify(resourceRepository).save(argThat(r -> r.getStatus() == ResourceStatus.ACTIVE));
        }

        @Test
        @DisplayName("Ném exception khi ngưng hoạt động nhưng có session tương lai")
        void shouldThrowExceptionWhenDeactivatingWithFutureSessions() {
            // Given
            Session futureSession = new Session();
            futureSession.setId(1L);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(sessionResourceRepository.findNextSessionByResourceId(eq(1L), any(), any()))
                    .thenReturn(futureSession);

            // When/Then
            assertThatThrownBy(() -> resourceService.updateResourceStatus(1L, ResourceStatus.INACTIVE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Không thể ngưng hoạt động vì tài nguyên này đang được sử dụng");
        }
    }

    // ==================== GET SESSIONS BY RESOURCE ID ====================
    @Nested
    @DisplayName("getSessionsByResourceId()")
    class GetSessionsByResourceIdTests {

        @Test
        @DisplayName("Trả về danh sách sessions sử dụng resource")
        void shouldReturnSessionsUsingResource() {
            // Given
            ClassEntity testClass = new ClassEntity();
            testClass.setId(1L);
            testClass.setCode("CLASS-001");
            testClass.setName("Test Class");

            TimeSlotTemplate timeSlot = new TimeSlotTemplate();
            timeSlot.setStartTime(LocalTime.of(9, 0));
            timeSlot.setEndTime(LocalTime.of(11, 0));

            Session session = new Session();
            session.setId(1L);
            session.setClassEntity(testClass);
            session.setDate(LocalDate.now());
            session.setTimeSlotTemplate(timeSlot);
            session.setStatus(org.fyp.tmssep490be.entities.enums.SessionStatus.PLANNED);
            session.setType(org.fyp.tmssep490be.entities.enums.SessionType.CLASS);

            when(resourceRepository.existsById(1L)).thenReturn(true);
            when(sessionResourceRepository.findSessionsByResourceId(1L)).thenReturn(List.of(session));

            // When
            List<SessionInfoDTO> result = resourceService.getSessionsByResourceId(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClassCode()).isEqualTo("CLASS-001");
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi resource không tồn tại")
        void shouldThrowResourceNotFoundExceptionWhenResourceNotExists() {
            // Given
            when(resourceRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> resourceService.getSessionsByResourceId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resource not found");
        }
    }
}
