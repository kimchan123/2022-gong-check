import useJobListCard from './useJobListCard';

import Button from '@/components/common/Button';
import JobBox from '@/components/host/JobListCard/JobBox';

import { JobType } from '@/types';

import emptyFolder from '@/assets/emptyFolder.png';

import styles from './styles';

interface JobListCardProps {
  jobs: JobType[];
}

const JobListCard: React.FC<JobListCardProps> = ({ jobs }) => {
  const { onClickNewJobButton } = useJobListCard();

  return (
    <div css={styles.layout}>
      <div css={styles.title}>
        <p>업무 목록</p>
        <Button css={styles.newJobButton} onClick={onClickNewJobButton}>
          새 업무 생성
        </Button>
      </div>
      <div css={styles.jobListWrapper}>
        {jobs.length === 0 ? (
          <div css={styles.empty}>
            <img src={emptyFolder} alt="" />
            <div>생성된 업무가 없습니다.</div>
          </div>
        ) : (
          jobs.map(job => <JobBox job={job} key={job.id} />)
        )}
      </div>
    </div>
  );
};

export default JobListCard;
