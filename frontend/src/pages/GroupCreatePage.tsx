import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { groupsApi } from '../api/endpoints';
import { GroupForm } from '../components/GroupForm';
import { useToast } from '../components/ui';
import { emptyGroup } from '../forms/model';

export function GroupCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  return (
    <div className="stack">
      <h1>Новая группа</h1>
      <GroupForm
        initial={emptyGroup()}
        submitLabel="Создать"
        onCancel={() => navigate('/')}
        onSubmit={async (request) => {
          const created = await groupsApi.create(request);
          await queryClient.invalidateQueries();
          toast(`Группа #${created.id} создана`);
          navigate(`/groups/${created.id}`);
        }}
      />
    </div>
  );
}
