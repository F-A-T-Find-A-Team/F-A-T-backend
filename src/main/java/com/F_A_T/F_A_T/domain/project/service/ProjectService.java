package com.F_A_T.F_A_T.domain.project.service;

import com.F_A_T.F_A_T.domain.application.repository.ProjectApplicationRepository;
import com.F_A_T.F_A_T.domain.chat.entity.ChatParticipant;
import com.F_A_T.F_A_T.domain.chat.entity.ChatRoom;
import com.F_A_T.F_A_T.domain.chat.repository.ChatParticipantRepository;
import com.F_A_T.F_A_T.domain.chat.repository.ChatRoomRepository;
import com.F_A_T.F_A_T.domain.project.dto.request.*;
import com.F_A_T.F_A_T.domain.project.dto.response.*;
import com.F_A_T.F_A_T.domain.project.entity.*;
import com.F_A_T.F_A_T.domain.project.repository.*;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ProjectApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createProject(User pm, ProjectCreateRequest request) {
        validateCreate(request);
        Project project = Project.builder().pm(pm).project_title(request.title())
                .project_description(request.description()).required_majors(request.requiredMajors())
                .required_stacks(request.techStacks()).project_deadline(request.deadline())
                .recruit_limit(request.capacity()).build();
        projectRepository.save(project);
        memberRepository.save(new ProjectMember(project, pm));
        var room = roomRepository.save(ChatRoom.builder().project(project).build());
        participantRepository.save(new ChatParticipant(room, pm));
        return project.getProject_id();
    }

    @Transactional
    public ProjectApiResponse.Created create(User actor, ProjectCreateRequest request) {
        Long id = createProject(actor, request);
        var p = requireProject(id);
        return new ProjectApiResponse.Created(id, p.getProject_title(), p.getProject_status(), recruitmentStatus(p, 1),
                actor.getUser_id(), 1, p.getRecruit_limit(), p.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ProjectApiResponse.Page<ProjectApiResponse.Card> search(String query, String recruitmentStatus,
            String major, String techStack, int page, int size) {
        validatePagination(page, size);
        validateFilter(query, recruitmentStatus, major, techStack);
        var result = projectRepository.findAll(filters(query, recruitmentStatus, major, techStack),
                PageRequest.of(page, size));
        return new ProjectApiResponse.Page<>(result.getContent().stream().map(this::card).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProjectApiResponse.Recommended recommended(User actor, int size) {
        if (size < 1 || size > 50) throw new ApiException(ErrorCode.INVALID_SIZE);
        User profile = userRepository.findById(actor.getUser_id()).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Set<String> majors = new HashSet<>(profile.getMajors());
        Set<String> stacks = new HashSet<>(profile.getInterest_stacks());
        List<Project> projects = projectRepository.findAll(filters(null, "OPEN", null, null));
        var items = projects.stream().filter(p -> !memberRepository.existsMember(p.getProject_id(), actor.getUser_id()))
                .sorted(Comparator.<Project>comparingInt(p -> score(p, majors, stacks)).reversed()
                        .thenComparing(Project::getProject_id, Comparator.reverseOrder()))
                .limit(size).map(this::card).toList();
        return new ProjectApiResponse.Recommended(items, "RULE_BASED");
    }

    @Transactional
    public ProjectApiResponse.Detail detail(User actor, Long id) {
        var p = requireLockedProject(id);
        p.increaseViewCount();
        int count = memberRepository.countMembers(id);
        boolean isPm = p.getPm().getUser_id().equals(actor.getUser_id());
        boolean member = memberRepository.existsMember(id, actor.getUser_id());
        var application = applicationRepository.findByProjectAndApplicantId(id, actor.getUser_id());
        return new ProjectApiResponse.Detail(id, p.getProject_title(), p.getProject_description(), pm(p),
                p.getProject_status(), recruitmentStatus(p, count), List.copyOf(p.getRequired_majors()),
                List.copyOf(p.getRequired_stacks()), p.getProject_deadline(), p.getRecruit_limit(), count,
                p.getView_count(), memberCards(p), new ProjectApiResponse.MyRelation(isPm, member,
                application.map(a -> a.getApplication_status().name()).orElse(null),
                !member && application.isEmpty() && isOpen(p, count), isPm && p.getProject_status() != ProjectStatus.COMPLETED));
    }

    @Transactional
    public ProjectApiResponse.Updated update(User actor, Long id, ProjectUpdateRequest r) {
        var p = requireLockedProject(id);
        requirePm(p, actor);
        if (r == null || (r.title() == null && r.description() == null && r.requiredMajors() == null &&
                r.techStacks() == null && r.deadline() == null && r.capacity() == null && r.recruitmentStatus() == null))
            throw new ApiException(ErrorCode.INVALID_PROJECT_DATA);
        if ((r.title() != null && (r.title().isBlank() || r.title().length() > 255)) ||
                (r.description() != null && r.description().isBlank()) ||
                (r.requiredMajors() != null && !validValues(r.requiredMajors())) ||
                (r.techStacks() != null && !validValues(r.techStacks())) ||
                (r.deadline() != null && !r.deadline().isAfter(LocalDate.now())) ||
                (r.capacity() != null && (r.capacity() < 1 || r.capacity() > 12)))
            throw new ApiException(ErrorCode.INVALID_PROJECT_DATA);
        if (r.capacity() != null && r.capacity() < memberRepository.countMembers(id))
            throw new ApiException(ErrorCode.CAPACITY_BELOW_MEMBER_COUNT);
        Boolean open = null;
        if (r.recruitmentStatus() != null) {
            if (!Set.of("OPEN", "CLOSED").contains(r.recruitmentStatus())) throw new ApiException(ErrorCode.INVALID_PROJECT_DATA);
            open = r.recruitmentStatus().equals("OPEN");
            if (open && (p.getProject_status() == ProjectStatus.COMPLETED ||
                    !(r.deadline() == null ? p.getProject_deadline() : r.deadline()).isAfter(LocalDate.now().minusDays(1))))
                throw new ApiException(ErrorCode.INVALID_PROJECT_DATA);
        }
        p.update(r.title(), r.description(), r.requiredMajors(), r.techStacks(), r.deadline(), r.capacity(), open);
        return new ProjectApiResponse.Updated(id, p.getProject_title(), p.getProject_description(), p.getRecruit_limit(), p.getProject_deadline());
    }

    @Transactional
    public ProjectApiResponse.StageChanged changeStage(User actor, Long id, ProjectStageRequest r) {
        var p = requireLockedProject(id);
        requirePm(p, actor);
        ProjectStatus next;
        try { next = ProjectStatus.valueOf(r.stage()); }
        catch (RuntimeException invalid) { throw new ApiException(ErrorCode.INVALID_STAGE); }
        if (next.ordinal() != p.getProject_status().ordinal() + 1)
            throw new ApiException(ErrorCode.INVALID_STAGE_TRANSITION);
        p.changeStatus(next);
        return new ProjectApiResponse.StageChanged(id, next, p.getCompletedAt(), next == ProjectStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public ProjectApiResponse.Members members(Long id) {
        var p = requireProject(id);
        return new ProjectApiResponse.Members(memberCards(p), p.getRecruit_limit());
    }

    // Compatibility for existing internal callers; HTTP controllers use the contracted API records above.
    @Transactional
    public ProjectResponse getProject(Long id) {
        var p = requireLockedProject(id); p.increaseViewCount();
        return ProjectResponse.from(p, memberRepository.countMembers(id));
    }
    @Transactional
    public void updateStatus(User actor, Long id, ProjectStatusUpdateRequest r) {
        var p = requireLockedProject(id); requirePm(p, actor);
        if (r == null || r.status() == null) throw new ApiException(ErrorCode.INVALID_STAGE);
        if (r.status().ordinal() < p.getProject_status().ordinal()) throw new ApiException(ErrorCode.INVALID_STAGE_TRANSITION);
        p.changeStatus(r.status());
    }
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long id) {
        var p = requireProject(id);
        return memberRepository.findMembers(id).stream().map(u -> ProjectMemberResponse.from(u, p.getPm().getUser_id())).toList();
    }
    @Transactional(readOnly = true)
    public List<ProjectListResponse> getProjectList() {
        return projectRepository.findAll().stream().map(p -> ProjectListResponse.from(p, memberRepository.countMembers(p.getProject_id()))).toList();
    }

    private void validateCreate(ProjectCreateRequest r) {
        if (r == null || r.title() == null || r.title().isBlank() || r.title().length() > 255 ||
                r.description() == null || r.description().isBlank() || !validValues(r.requiredMajors()) ||
                !validValues(r.techStacks()) || r.deadline() == null || r.capacity() == null)
            throw new ApiException(ErrorCode.INVALID_PROJECT_DATA);
        if (!r.deadline().isAfter(LocalDate.now())) throw new ApiException(ErrorCode.INVALID_DEADLINE);
        if (r.capacity() < 1 || r.capacity() > 12) throw new ApiException(ErrorCode.INVALID_CAPACITY);
    }
    private boolean validValues(List<String> values) {
        return values != null && !values.isEmpty() && values.size() <= 50 &&
                values.stream().allMatch(v -> v != null && !v.isBlank() && v.length() <= 255) &&
                new HashSet<>(values).size() == values.size();
    }
    public static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > 1_000_000) throw new ApiException(ErrorCode.INVALID_PAGINATION);
    }
    private void validateFilter(String query, String status, String major, String stack) {
        if ((status != null && !Set.of("OPEN", "CLOSED").contains(status)) ||
                (query != null && query.length() > 255) || (major != null && (major.isBlank() || major.length() > 255)) ||
                (stack != null && (stack.isBlank() || stack.length() > 255))) throw new ApiException(ErrorCode.INVALID_FILTER);
    }
    private Specification<Project> filters(String query, String status, String major, String stack) {
        return (root, cq, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String escaped = query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                conditions.add(cb.or(cb.like(cb.lower(root.get("project_title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("project_description")), pattern, '\\')));
            }
            if (major != null) conditions.add(cb.isMember(major, root.get("required_majors")));
            if (stack != null) conditions.add(cb.isMember(stack, root.get("required_stacks")));
            if (status != null) {
                Subquery<Long> members = cq.subquery(Long.class);
                Root<ProjectMember> member = members.from(ProjectMember.class);
                members.select(cb.count(member)).where(cb.equal(member.get("project"), root));
                Predicate open = cb.and(cb.isTrue(root.get("recruiting_open")),
                        cb.notEqual(root.get("project_status"), ProjectStatus.COMPLETED),
                        cb.greaterThanOrEqualTo(root.get("project_deadline"), LocalDate.now()),
                        cb.lt(members, root.get("recruit_limit")));
                conditions.add(status.equals("OPEN") ? open : cb.not(open));
            }
            if (!Long.class.equals(cq.getResultType()) && !long.class.equals(cq.getResultType())) {
                cq.orderBy(cb.desc(root.get("project_id")));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
    private ProjectApiResponse.Card card(Project p) {
        int count = memberRepository.countMembers(p.getProject_id());
        String description = p.getProject_description();
        return new ProjectApiResponse.Card(p.getProject_id(), p.getProject_title(),
                description.length() > 140 ? description.substring(0, 140) : description, pm(p), p.getProject_status(),
                recruitmentStatus(p, count), List.copyOf(p.getRequired_majors()), List.copyOf(p.getRequired_stacks()),
                p.getProject_deadline(), count, p.getRecruit_limit(), p.getView_count());
    }
    private ProjectApiResponse.Pm pm(Project p) { return new ProjectApiResponse.Pm(p.getPm().getUser_id(), p.getPm().getNickname()); }
    private List<ProjectApiResponse.Member> memberCards(Project p) {
        return memberRepository.findMembers(p.getProject_id()).stream().map(u -> new ProjectApiResponse.Member(
                u.getUser_id(), u.getNickname(), p.getPm().getUser_id().equals(u.getUser_id()) ? "PM" : "MEMBER",
                List.copyOf(u.getMajors()), u.getAvatar_url())).toList();
    }
    private int score(Project p, Set<String> majors, Set<String> stacks) {
        return (int) p.getRequired_majors().stream().filter(majors::contains).count() * 2 +
                (int) p.getRequired_stacks().stream().filter(stacks::contains).count();
    }
    private boolean isOpen(Project p, int count) {
        return p.isRecruiting_open() && p.getProject_status() != ProjectStatus.COMPLETED &&
                !p.getProject_deadline().isBefore(LocalDate.now()) && count < p.getRecruit_limit();
    }
    private String recruitmentStatus(Project p, int count) { return isOpen(p, count) ? "OPEN" : "CLOSED"; }
    private Project requireProject(Long id) {
        validateId(id);
        return projectRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
    }
    private Project requireLockedProject(Long id) {
        validateId(id);
        return projectRepository.findForUpdate(id).orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
    }
    private void validateId(Long id) { if (id == null || id < 1) throw new ApiException(ErrorCode.INVALID_PROJECT_ID); }
    private void requirePm(Project p, User actor) {
        if (!p.getPm().getUser_id().equals(actor.getUser_id())) throw new ApiException(ErrorCode.NOT_PROJECT_PM);
    }
}
