import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Users, UserPlus, Trash2, ChevronLeft, Mail, Shield, X } from 'lucide-react';
import { eventApi } from '../../api/endpoints';
import { EventMember } from '../../types';
import { formatDateTime, getInitials } from '../../utils/helpers';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const memberSchema = z.object({
  userId: z.string().uuid('Invalid user ID'),
});

type MemberForm = z.infer<typeof memberSchema>;

export function AdminEventMembersPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const queryClient = useQueryClient();
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MemberForm>({
    resolver: zodResolver(memberSchema),
  });

  const { data: members } = useQuery({
    queryKey: ['event-members', eventId],
    queryFn: () => eventApi.getMembers(eventId!),
    enabled: !!eventId,
  });

  const addMemberMutation = useMutation({
    mutationFn: (data: MemberForm) => eventApi.addMember(eventId!, data),
    onSuccess: () => {
      toast.success('Member added successfully');
      setShowAddMemberModal(false);
      reset();
      queryClient.invalidateQueries({ queryKey: ['event-members', eventId] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to add member');
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: (memberId: string) => eventApi.removeMember(eventId!, memberId),
    onSuccess: () => {
      toast.success('Member removed');
      queryClient.invalidateQueries({ queryKey: ['event-members', eventId] });
      setDeleteConfirm(null);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to remove member');
      setDeleteConfirm(null);
    },
  });

  const handleAddMember = (data: MemberForm) => {
    addMemberMutation.mutate(data);
  };

  const handleRemoveMember = (memberId: string) => {
    if (!deleteConfirm) {
      setDeleteConfirm(memberId);
      return;
    }
    removeMemberMutation.mutate(memberId);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link to={`/events/${eventId}`} className="p-2 rounded-lg text-gray-500 hover:bg-gray-100">
            <ChevronLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Team Members</h1>
            <p className="text-gray-600">Manage team members for this event</p>
          </div>
        </div>
        <button
          onClick={() => setShowAddMemberModal(true)}
          className="btn-primary flex items-center gap-2"
        >
          <UserPlus className="h-5 w-5" />
          Add Member
        </button>
      </div>

      <div className="card">
        <div className="divide-y divide-gray-200">
          {members?.length === 0 ? (
            <div className="p-12 text-center">
              <Users className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No team members yet</h3>
              <p className="text-gray-500 mb-6">Add team members to allow them to upload photos</p>
              <button
                onClick={() => setShowAddMemberModal(true)}
                className="btn-primary"
              >
                <UserPlus className="h-5 w-5 mr-2" />
                Add Member
              </button>
            </div>
          ) : (
            members?.map((member: EventMember) => (
              <div key={member.id} className="p-6 flex items-center justify-between hover:bg-gray-50">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-indigo-100 flex items-center justify-center">
                    <span className="font-medium text-indigo-700">{getInitials(member.userName)}</span>
                  </div>
                  <div>
                    <p className="font-medium text-gray-900">{member.userName}</p>
                    <p className="text-sm text-gray-500 flex items-center gap-2">
                      <Mail className="h-4 w-4" />
                      {member.userEmail}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="badge-info flex items-center gap-1">
                    <Shield className="h-3 w-3" />
                    Team Member
                  </span>
                  <span className="text-sm text-gray-500">Since {formatDateTime(member.assignedAt)}</span>
                  <button
                    onClick={() => handleRemoveMember(member.userId)}
                    disabled={deleteConfirm === member.userId || addMemberMutation.isPending}
                    className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50 transition-colors"
                  >
                    {deleteConfirm === member.userId ? (
                      <span className="text-sm text-red-600">Confirm?</span>
                    ) : (
                      <Trash2 className="h-5 w-5" />
                    )}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {showAddMemberModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50" onClick={() => setShowAddMemberModal(false)} />
            <div className="relative bg-white rounded-xl shadow-xl max-w-md w-full p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-gray-900">Add Team Member</h2>
                <button
                  onClick={() => setShowAddMemberModal(false)}
                  className="p-1 rounded-lg text-gray-500 hover:bg-gray-100"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleSubmit(handleAddMember)} className="space-y-4">
                <div>
                  <label htmlFor="userId" className="label">User ID (UUID)</label>
                  <input
                    id="userId"
                    type="text"
                    {...register('userId')}
                    className="input"
                    placeholder="Enter user UUID"
                    disabled={addMemberMutation.isPending}
                  />
                  {errors.userId && <p className="text-sm text-red-600 mt-1">{errors.userId.message}</p>}
                  <p className="text-xs text-gray-500 mt-1">Enter the UUID of an existing team member user</p>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowAddMemberModal(false)}
                    className="btn-secondary flex-1"
                    disabled={addMemberMutation.isPending}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={addMemberMutation.isPending}
                    className="btn-primary flex-1"
                  >
                    {addMemberMutation.isPending ? 'Adding...' : 'Add Member'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}