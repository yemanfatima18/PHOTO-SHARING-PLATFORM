import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Calendar, Image, Camera, ArrowRight } from 'lucide-react';
import { eventApi } from '../../api/endpoints';
import { Event } from '../../types';
import { formatDate } from '../../utils/helpers';
import { useState } from 'react';

export function TeamDashboardPage() {
  const [page, setPage] = useState(0);

  const { data: eventsData, isLoading, error } = useQuery({
    queryKey: ['team-events', page],
    queryFn: () => eventApi.getMyEvents(page, 10),
  });

  const events = eventsData?.content || [];
  const totalPages = eventsData?.totalPages || 1;

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="card p-12 text-center text-red-600">Failed to load events</div>
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="card p-12 text-center">
          <Camera className="h-16 w-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No events assigned</h3>
          <p className="text-gray-500">Contact your admin to be assigned to events</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">My Events</h1>
        <p className="text-gray-600 mt-1">Events assigned to you</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {events.map((event: Event) => (
          <Link
            key={event.id}
            to={`/team/events/${event.id}`}
            className="card p-6 hover:shadow-md transition-shadow h-full flex flex-col"
          >
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-indigo-100 rounded-xl">
                <Calendar className="h-8 w-8 text-indigo-600" />
              </div>
              <span className="badge-info">Team Member</span>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2 flex-1">{event.name}</h3>
            <p className="text-gray-600 text-sm mb-4 line-clamp-2">{event.description || 'No description'}</p>
            <div className="flex items-center gap-4 text-sm text-gray-500 mb-4">
              <span className="flex items-center gap-1">
                <Calendar className="h-4 w-4" />
                {formatDate(event.eventDate)}
              </span>
              <span className="flex items-center gap-1">
                <Image className="h-4 w-4" />
                {event.photoCount} photos
              </span>
            </div>
            <div className="mt-auto pt-4 border-t border-gray-200">
              <span className="btn-primary w-full text-center flex items-center justify-center gap-2">
                View Event
                <ArrowRight className="h-4 w-4" />
              </span>
            </div>
          </Link>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-2">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="btn-secondary"
          >
            Previous
          </button>
          <span className="text-gray-600">Page {page + 1} of {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="btn-secondary"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}