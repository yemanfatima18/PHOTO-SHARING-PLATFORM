import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Camera, Calendar, Image, Plus, ExternalLink, X } from 'lucide-react';
import { eventApi } from '../../api/endpoints';
import { Event } from '../../types';
import { formatDate } from '../../utils/helpers';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const eventSchema = z.object({
  name: z.string().min(2, 'Event name must be at least 2 characters').max(255),
  description: z.string().max(2000).optional(),
  eventDate: z.string().min(1, 'Event date is required'),
});

type EventForm = z.infer<typeof eventSchema>;

export function AdminDashboardPage() {
  const [page, setPage] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      eventDate: new Date().toISOString().split('T')[0],
    },
  });

  const { data: eventsData, isLoading, error, refetch } = useQuery({
    queryKey: ['events', page],
    queryFn: () => eventApi.getMyEvents(page, 10),
  });

  const onCreateEvent = async (data: EventForm) => {
    setIsSubmitting(true);
    try {
      await eventApi.create(data);
      toast.success('Event created successfully');
      setShowCreateModal(false);
      reset();
      refetch();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create event');
    } finally {
      setIsSubmitting(false);
    }
  };

  const events = eventsData?.content || [];
  const totalPages = eventsData?.totalPages || 1;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">Manage your events and team members</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="h-5 w-5" />
          Create Event
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Events</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{eventsData?.totalElements || 0}</p>
            </div>
            <div className="p-3 bg-indigo-100 rounded-xl">
              <Calendar className="h-8 w-8 text-indigo-600" />
            </div>
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Photos</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {events.reduce((sum, e) => sum + e.photoCount, 0)}
              </p>
            </div>
            <div className="p-3 bg-green-100 rounded-xl">
              <Image className="h-8 w-8 text-green-600" />
            </div>
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Selected Photos</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {events.reduce((sum, e) => sum + e.selectedPhotoCount, 0)}
              </p>
            </div>
            <div className="p-3 bg-yellow-100 rounded-xl">
              <Image className="h-8 w-8 text-yellow-600" />
            </div>
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Published Galleries</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {events.filter(e => e.galleryPublished).length}
              </p>
            </div>
            <div className="p-3 bg-purple-100 rounded-xl">
              <ExternalLink className="h-8 w-8 text-purple-600" />
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Your Events</h2>
        </div>

        {isLoading ? (
          <div className="p-6 text-center">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent mx-auto"></div>
          </div>
        ) : error ? (
          <div className="p-6 text-center text-red-600">Failed to load events</div>
        ) : events.length === 0 ? (
          <div className="p-12 text-center">
            <Camera className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No events yet</h3>
            <p className="text-gray-500 mb-6">Create your first event to get started</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn-primary"
            >
              <Plus className="h-5 w-5 mr-2" />
              Create Event
            </button>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Event</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Photos</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Selected</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Gallery</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {events.map((event: Event) => (
                    <tr key={event.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <Link to={`/events/${event.id}`} className="font-medium text-indigo-600 hover:text-indigo-900">
                          {event.name}
                        </Link>
                        <p className="text-sm text-gray-500 truncate max-w-xs">{event.description || 'No description'}</p>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {formatDate(event.eventDate)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {event.photoCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {event.selectedPhotoCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {event.galleryPublished ? (
                          <span className="badge-success">Published</span>
                        ) : event.hasGallery ? (
                          <span className="badge-warning">Draft</span>
                        ) : (
                          <span className="badge">Not created</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <Link to={`/events/${event.id}`} className="text-indigo-600 hover:text-indigo-900 mr-4">
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                <p className="text-sm text-gray-600">
                  Page {page + 1} of {totalPages} ({eventsData?.totalElements} events)
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary text-sm"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page === totalPages - 1}
                    className="btn-secondary text-sm"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {showCreateModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50" onClick={() => setShowCreateModal(false)} />
            <div className="relative bg-white rounded-xl shadow-xl max-w-md w-full p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-gray-900">Create Event</h2>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="p-1 rounded-lg text-gray-500 hover:bg-gray-100"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleSubmit(onCreateEvent)} className="space-y-4">
                <div>
                  <label htmlFor="name" className="label">Event Name</label>
                  <input
                    id="name"
                    type="text"
                    {...register('name')}
                    className="input"
                    placeholder="Arjun & Priya Wedding"
                    disabled={isSubmitting}
                  />
                  {errors.name && <p className="text-sm text-red-600 mt-1">{errors.name.message}</p>}
                </div>

                <div>
                  <label htmlFor="description" className="label">Description (optional)</label>
                  <textarea
                    id="description"
                    {...register('description')}
                    className="input min-h-[100px]"
                    placeholder="Event details..."
                    disabled={isSubmitting}
                  />
                </div>

                <div>
                  <label htmlFor="eventDate" className="label">Event Date</label>
                  <input
                    id="eventDate"
                    type="date"
                    {...register('eventDate')}
                    className="input"
                    disabled={isSubmitting}
                  />
                  {errors.eventDate && <p className="text-sm text-red-600 mt-1">{errors.eventDate.message}</p>}
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="btn-secondary flex-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="btn-primary flex-1"
                  >
                    {isSubmitting ? 'Creating...' : 'Create Event'}
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