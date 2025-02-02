package com.woowacourse.gongcheck.core.application;

import static com.woowacourse.gongcheck.fixture.FixtureFactory.Host_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.Job_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.RunningTask_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.Section_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.Space_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.Submission_생성;
import static com.woowacourse.gongcheck.fixture.FixtureFactory.Task_생성;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.woowacourse.gongcheck.ApplicationTest;
import com.woowacourse.gongcheck.SupportRepository;
import com.woowacourse.gongcheck.core.application.response.SubmissionResponse;
import com.woowacourse.gongcheck.core.application.response.SubmissionsResponse;
import com.woowacourse.gongcheck.core.domain.host.Host;
import com.woowacourse.gongcheck.core.domain.job.Job;
import com.woowacourse.gongcheck.core.domain.section.Section;
import com.woowacourse.gongcheck.core.domain.space.Space;
import com.woowacourse.gongcheck.core.domain.submission.Submission;
import com.woowacourse.gongcheck.core.domain.submission.SubmissionRepository;
import com.woowacourse.gongcheck.core.domain.task.RunningTaskRepository;
import com.woowacourse.gongcheck.core.domain.task.RunningTaskSseEmitterContainer;
import com.woowacourse.gongcheck.core.domain.task.Task;
import com.woowacourse.gongcheck.core.presentation.request.SubmissionRequest;
import com.woowacourse.gongcheck.exception.BusinessException;
import com.woowacourse.gongcheck.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

@ApplicationTest
@DisplayName("SubmissionService 클래스")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubmissionServiceTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SupportRepository repository;

    @Autowired
    private RunningTaskRepository runningTaskRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @MockBean
    private RunningTaskSseEmitterContainer runningTaskSseEmitterContainer;

    @Nested
    class submitJobCompletion_메소드는 {

        @Nested
        class 입력받은_Host가_존재하지_않는_경우 {

            private static final long NON_EXIST_HOST_ID = 0L;

            private Long jobId;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                jobId = 1L;
                request = new SubmissionRequest("제출자");
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.submitJobCompletion(NON_EXIST_HOST_ID, jobId, request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 호스트입니다.");
            }
        }

        @Nested
        class 입력받은_Job이_존재하지_않는_경우 {

            private static final long NON_EXIST_JOB_ID = 0L;

            private Host host;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                request = new SubmissionRequest("제출자");
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.submitJobCompletion(host.getId(), NON_EXIST_JOB_ID, request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 작업입니다.");
            }
        }

        @Nested
        class 다른_Host의_Job을_입력받는_경우 {

            private Host anotherHost;
            private Job job;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                Host host = repository.save(Host_생성("1234", 1234L));
                anotherHost = repository.save(Host_생성("1234", 2345L));
                Space space = repository.save(Space_생성(host, "잠실"));
                job = repository.save(Job_생성(space, "청소"));
                request = new SubmissionRequest("제출자");
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(
                        () -> submissionService.submitJobCompletion(anotherHost.getId(), job.getId(), request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 작업입니다.");
            }
        }

        @Nested
        class RunningTask가_존재하지_않는_경우 {

            private Host host;
            private Job job;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                Space space = repository.save(Space_생성(host, "잠실"));
                job = repository.save(Job_생성(space, "청소"));
                request = new SubmissionRequest("제출자");
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.submitJobCompletion(host.getId(), job.getId(), request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("현재 제출할 수 있는 진행중인 작업이 존재하지 않습니다.");
            }
        }

        @Nested
        class 모든_RunningTask가_체크상태가_아닌_경우 {

            private Host host;
            private Job job;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                Space space = repository.save(Space_생성(host, "잠실"));
                job = repository.save(Job_생성(space, "청소"));
                Section section = repository.save(Section_생성(job, "트랙룸"));
                Task task_1 = repository.save(Task_생성(section, "책상 청소"));
                Task task_2 = repository.save(Task_생성(section, "의자 넣기"));
                repository.save(RunningTask_생성(task_1.getId(), false));
                repository.save(RunningTask_생성(task_2.getId(), false));
                request = new SubmissionRequest("제출자");
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.submitJobCompletion(host.getId(), job.getId(), request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("모든 작업이 완료되지않아 제출이 불가합니다.");
            }
        }

        @Nested
        class 모든_RunningTask가_체크_상태인_경우 {

            private Host host;
            private Space space;
            private Job job;
            private SubmissionRequest request;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                space = repository.save(Space_생성(host, "잠실"));
                job = repository.save(Job_생성(space, "청소"));
                Section section = repository.save(Section_생성(job, "트랙룸"));
                Task task_1 = repository.save(Task_생성(section, "책상 청소"));
                Task task_2 = repository.save(Task_생성(section, "의자 넣기"));
                repository.save(RunningTask_생성(task_1.getId(), true));
                repository.save(RunningTask_생성(task_2.getId(), true));
                request = new SubmissionRequest("제출자");
            }

            @Test
            void Submission을_생성한다() {
                submissionService.submitJobCompletion(host.getId(), job.getId(), request);
                List<Submission> submissions = submissionRepository.findAll();
                int runningTaskSize = runningTaskRepository.findAll()
                        .size();

                assertAll(
                        () -> assertThat(submissions).hasSize(1),
                        () -> assertThat(submissions.get(0).getAuthor()).isEqualTo(request.getAuthor()),
                        () -> assertThat(runningTaskSize).isZero()
                );
            }

            @Test
            void Submission_Sse_Event를_발행한다() {
                submissionService.submitJobCompletion(host.getId(), job.getId(), request);

                verify(runningTaskSseEmitterContainer, times(1))
                        .publishSubmitEvent(anyLong());
            }
        }
    }

    @Nested
    class findPage_메소드는 {

        @Nested
        class 입력받은_Host가_존재하지_않는_경우 {

            private static final long NON_EXIST_HOST_ID = 0L;

            private Long spaceId;
            private PageRequest request;

            @BeforeEach
            void setUp() {
                spaceId = 1L;
                request = PageRequest.of(0, 2);
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.findPage(NON_EXIST_HOST_ID, spaceId, request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 호스트입니다.");
            }
        }

        @Nested
        class 입력받은_Space가_존재하지_않는_경우 {

            private static final long NON_EXIST_SPACE_ID = 0L;

            private Host host;
            private PageRequest request;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                request = PageRequest.of(0, 2);
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(() -> submissionService.findPage(host.getId(), NON_EXIST_SPACE_ID, request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 공간입니다.");
            }
        }

        @Nested
        class 다른_Host의_Space를_입력받는_경우 {

            private Host anotherHost;
            private Space space;
            private PageRequest request;

            @BeforeEach
            void setUp() {
                Host host = repository.save(Host_생성("1234", 1234L));
                anotherHost = repository.save(Host_생성("1234", 2345L));
                space = repository.save(Space_생성(host, "잠실"));
                request = PageRequest.of(0, 2);
            }

            @Test
            void 예외를_발생시킨다() {
                assertThatThrownBy(
                        () -> submissionService.findPage(anotherHost.getId(), space.getId(), request))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("존재하지 않는 공간입니다.");
            }
        }

        @Nested
        class 올바른_Host의_Space를_입력받는_경우 {

            private static final String SUBMISSION_AUTHOR_1 = "어썸오";
            private static final String SUBMISSION_AUTHOR_2 = "어썸오";

            private Host host;
            private Space space;
            private PageRequest request;
            private Job job;

            @BeforeEach
            void setUp() {
                host = repository.save(Host_생성("1234", 1234L));
                space = repository.save(Space_생성(host, "잠실"));
                job = repository.save(Job_생성(space, "청소"));
                request = PageRequest.of(0, 2);
                repository.saveAll(
                        List.of(Submission_생성(job, SUBMISSION_AUTHOR_1), Submission_생성(job, SUBMISSION_AUTHOR_2),
                                Submission_생성(job, SUBMISSION_AUTHOR_2)));
            }

            @Test
            void Submission을_조회한다() {
                SubmissionsResponse actual = submissionService.findPage(host.getId(), space.getId(), request);

                assertAll(
                        () -> assertThat(actual.getSubmissions())
                                .extracting(SubmissionResponse::getJobId)
                                .containsExactly(job.getId(), job.getId()),
                        () -> assertThat(actual.getSubmissions())
                                .extracting(SubmissionResponse::getJobName)
                                .containsExactly(job.getName().getValue(), job.getName().getValue()),
                        () -> assertThat(actual.getSubmissions())
                                .extracting(SubmissionResponse::getAuthor)
                                .containsExactly(SUBMISSION_AUTHOR_1, SUBMISSION_AUTHOR_2),
                        () -> assertThat(actual.isHasNext()).isTrue()
                );
            }
        }
    }
}
