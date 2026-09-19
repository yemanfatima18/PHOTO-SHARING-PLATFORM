import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Camera, Users, Image, ArrowLeft, Trash2, Plus, ExternalLink, X } from 'lucide-react';
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

export function AdminEventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MemberForm>({
    resolver: zodResolver(memberSchema),
  });

  const { data: event, isLoading: eventLoading, error: eventError, refetch: refetchEvent } = useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventApi.getById(eventId!),
    enabled: !!eventId,
  });

  const { data: members, refetch: refetchMembers } = useQuery({
    queryKey: ['event-members', eventId],
    queryFn: () => eventApi.getMembers(eventId!),
    enabled: !!eventId,
  });

  const handleAddMember = async (data: MemberForm) => {
    setIsSubmitting(true);
    try {
      await eventApi.addMember(eventId!, data);
      toast.success('Member added successfully');
      setShowAddMemberModal(false);
      reset();
      refetchMembers();
      refetchEvent();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add member');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!deleteConfirm) {
      setDeleteConfirm(memberId);
      return;
    }
    try {
      await eventApi.removeMember(eventId!, memberId);
      toast.success('Member removed');
      refetchMembers();
      refetchEvent();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to remove member');
    } finally {
      setDeleteConfirm(null);
    }
  };

  if (eventLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent"></div>
        </div>
      </div>
    );
  }

  if (eventError || !event) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-center">
        <Camera className="h-16 w-16 text-gray-300 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-gray-900">Event not found</h2>
        <Link to="/events" className="text-indigo-600 hover:text-indigo-500 mt-4 inline-block">
          Back to events
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 rounded-lg text-gray-500 hover:bg-gray-100">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{event.name}</h1>
          <p className="text-gray-600">{event.description || 'No description'}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <Link to={`/events/${event.id}/photos`} className="card p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Photos</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{event.photoCount}</p>
            </div>
            <div className="p-3 bg-indigo-100 rounded-xl">
              <Image className="h-8 w-8 text-indigo-600" />
            </div>
          </div>
        </Link>

        <Link to={`/events/${event.id}/members`} className="card p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Team Members</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{members?.length || 0}</p>
            </div>
            <div className="p-3 bg-green-100 rounded-xl">
              <Users className="h-8 w-8 text-green-600" />
            </div>
          </div>
        </Link>

        <Link to={`/events/${event.id}/gallery`} className="card p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Gallery</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {event.galleryPublished ? 'Published' : event.hasGallery ? 'Draft' : 'Not created'}
              </p>
            </div>
            <div className="p-3 bg-purple-100 rounded-xl">
              <ExternalLink className="h-8 w-8 text-purple-600" />
            </div>
          </div>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="p-6 border-b border-gray-200 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Event Details</h2>
          </div>
          <div className="p-6 space-y-4">
            <div>
              <p className="text-sm text-gray-500">Date</p>
              <p className="font-medium text-gray-900">{formatDateTime(event.eventDate)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Created by</p>
              <p className="font-medium text-gray-900">{event.createdByName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Created at</p>
              <p className="font-medium text-gray-900">{formatDateTime(event.createdAt)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Last updated</p>
              <p className="font-medium text-gray-900">{formatDateTime(event.updatedAt)}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="p-6 border-b border-gray-200 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Team Members</h2>
            <button
              onClick={() => setShowAddMemberModal(true)}
              className="btn-primary text-sm flex items-center gap-1"
            >
              <Plus className="h-4 w-4" />
              Add Member
            </button>
          </div>
          <div className="divide-y divide-gray-200">
            {members?.length === 0 ? (
              <div className="p-6 text-center text-gray-500">
                <Users className="h-12 w-12 text-gray-300 mx-auto mb-2" />
                <p>No team members yet</p>
                <button onClick={() => setShowAddMemberModal(true)} className="text-indigo-600 hover:text-indigo-500 text-sm font-medium mt-2 inline-block">
                  Add first member
                </button>
              </div>
            ) : (
              members?.map((member: EventMember) => (
                <div key={member.id} className="p-4 flex items-center justify-between hover:bg-gray-50">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center">
                      <span className="font-medium text-indigo-700">{getInitials(member.userName)}</span>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">{member.userName}</p>
                      <p className="text-sm text-gray-500">{member.userEmail}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="badge-info">Team Member</span>
                    <button
                      onClick={() => handleRemoveMember(member.userId)}
                      disabled={deleteConfirm === member.userId}
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
                    disabled={isSubmitting}
                  />
                  {errors.userId && <p className="text-sm text-red-600 mt-1">{errors.userId.message}</p>}
                  <p className="text-xs text-gray-500 mt-1">Enter the UUID of an existing team member user</p>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowAddMemberModal(false)}
                    className="btn-secondary flex-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="btn-primary flex-1"
                  >
                    {isSubmitting ? 'Adding...' : 'Add Member'}
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