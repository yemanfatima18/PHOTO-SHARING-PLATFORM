import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Image, Check, CheckSquare, Square, ChevronLeft, ChevronRight, Eye, X } from 'lucide-react';
import { photoApi } from '../../api/endpoints';
import { Photo } from '../../types';
import { formatFileSize, formatDateTime, cn } from '../../utils/helpers';
import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';

export function AdminEventPhotosPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selectedPhotos, setSelectedPhotos] = useState<Set<string>>(new Set());
  const [selectAll, setSelectAll] = useState(false);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [selectedPhotoForView, setSelectedPhotoForView] = useState<Photo | null>(null);
  const [photoUrls, setPhotoUrls] = useState<Map<string, string>>(new Map());

  const { data: photosData, isLoading, error } = useQuery({
    queryKey: ['event-photos', eventId, page],
    queryFn: () => photoApi.getEventPhotos(eventId!, page, 20),
    enabled: !!eventId,
  });

  // Generate photo URLs for display
  useEffect(() => {
    if (photosData?.content) {
      const newUrls = new Map<string, string>();
      photosData.content.forEach(photo => {
        const url = `/api/events/${eventId}/photos/${photo.id}/download`;
        newUrls.set(photo.id, url);
      });
      setPhotoUrls(newUrls);
    }
  }, [photosData?.content, eventId]);

  const selectMutation = useMutation({
    mutationFn: ({ photoIds, selected }: { photoIds: string[]; selected: boolean }) =>
      photoApi.updateSelection(eventId!, { photoIds, selected }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event-photos', eventId] });
      setSelectedPhotos(new Set());
      setSelectAll(false);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to update selection');
    },
  });

  const photos = photosData?.content || [];
  const totalPages = photosData?.totalPages || 1;

  useEffect(() => {
    if (selectAll) {
      setSelectedPhotos(new Set(photos.map(p => p.id)));
    } else {
      setSelectedPhotos(new Set());
    }
  }, [photos, selectAll]);

  const toggleSelectAll = () => {
    setSelectAll(!selectAll);
  };

  const togglePhoto = (photoId: string) => {
    const newSelected = new Set(selectedPhotos);
    if (newSelected.has(photoId)) {
      newSelected.delete(photoId);
    } else {
      newSelected.add(photoId);
    }
    setSelectedPhotos(newSelected);
    setSelectAll(newSelected.size === photos.length && photos.length > 0);
  };

  const handleBulkSelect = (selected: boolean) => {
    const photoIds = Array.from(selectedPhotos);
    if (photoIds.length === 0) return;
    selectMutation.mutate({ photoIds, selected });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link to={`/events/${eventId}`} className="p-2 rounded-lg text-gray-500 hover:bg-gray-100">
            <ChevronLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Photos</h1>
            <p className="text-gray-600">Review and select photos for gallery</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 border border-gray-300 rounded-lg p-1">
            <button
              onClick={() => setViewMode('grid')}
              className={cn('p-2 rounded transition-colors', viewMode === 'grid' ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:text-gray-900')}
            >
              <Image className="h-5 w-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={cn('p-2 rounded transition-colors', viewMode === 'list' ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:text-gray-900')}
            >
              <span className="text-sm font-medium">List</span>
            </button>
          </div>

          {selectedPhotos.size > 0 && (
            <div className="flex items-center gap-2">
              <button
                onClick={() => handleBulkSelect(true)}
                disabled={selectedPhotos.size === photos.length}
                className="btn-secondary text-sm"
              >
                <CheckSquare className="h-4 w-4 mr-1" />
                Select ({selectedPhotos.size})
              </button>
              <button
                onClick={() => handleBulkSelect(false)}
                disabled={selectedPhotos.size === 0}
                className="btn-secondary text-sm"
              >
                <Square className="h-4 w-4 mr-1" />
                Deselect
              </button>
            </div>
          )})
        </div>
      </div>

      <div className="card">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent"></div>
          </div>
        ) : error ? (
          <div className="card p-12 text-center text-red-600">Failed to load photos</div>
        ) : photos.length === 0 ? (
          <div className="card p-12 text-center">
            <Image className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No photos uploaded yet</h3>
            <p className="text-gray-500">Team members can upload photos to this event</p>
          </div>
        ) : (
          <div>
            {viewMode === 'grid' ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                {photos.map((photo: Photo) => {
                  return (
                    <div
                      key={photo.id}
                      className={cn(
                        'relative group cursor-pointer rounded-lg overflow-hidden border-2 transition-colors',
                        selectedPhotos.has(photo.id) ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'
                      )}
                      onClick={() => togglePhoto(photo.id)}
                    >
                      <div className="aspect-[4/3] bg-gray-100 relative overflow-hidden">
                        {photoUrls.get(photo.id) ? (
                          <div>
                            <img
                              src={photoUrls.get(photo.id)!}
                              alt={photo.originalFilename}
                              className="w-full h-full object-cover"
                              loading="lazy"
                            />
                          </div>
                        ) : (
                          ''
                        )}
                        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                          <Eye className="h-8 w-8 text-white" />
                        </div>
                      </div>
                      <div className="absolute top-2 right-2">
                        <button
                          onClick={(e) => { e.stopPropagation(); togglePhoto(photo.id); }}
                          className={cn(
                            'p-2 rounded-full transition-colors',
                            selectedPhotos.has(photo.id)
                              ? 'bg-indigo-600 text-white'
                              : 'bg-white/90 text-gray-600 hover:bg-white'
                          )}
                        >
                          {selectedPhotos.has(photo.id) ? (
                            <Check className="h-5 w-5" />
                          ) : (
                            <Square className="h-5 w-5" />
                          )}
                        </button>
                      </div>
                      {photo.selectedForPublishing ? (
                        <div className="absolute bottom-2 left-2">
                          <span className="badge-success">Selected</span>
                        </div>
                      ) : (
                        ''
                      )}
                    </div>
                  );
                }
                )}
                </div>
              ) : (
                <div className="card overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 w-12"><input type="checkbox" checked={selectAll} onChange={toggleSelectAll} className="rounded border-gray-300" /></th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Preview</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Filename</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Uploader</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Size</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Uploaded</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      {photos.map((photo: Photo) => (
                        <tr key={photo.id} className={cn(selectedPhotos.has(photo.id) ? 'bg-indigo-50' : 'hover:bg-gray-50')}>
                          <td className="px-6 py-4">
                            <input
                              type="checkbox"
                              checked={selectedPhotos.has(photo.id)}
                              onChange={() => togglePhoto(photo.id)}
                              className="rounded border-gray-300"
                            />
                          </td>
                          <td className="px-6 py-4">
                            {photoUrls.get(photo.id) ? (
                              <img src={photoUrls.get(photo.id)!} alt={photo.originalFilename} className="w-16 h-16 object-cover rounded" />
                            ) : (
                              ''
                            )}
                          </td>
                          <td className="px-6 py-4">
                            <p className="font-medium text-gray-900 truncate max-w-xs">{photo.originalFilename}</p>
                          </td>
                          <td className="px-6 py-4 text-sm text-gray-900">{photo.uploadedByName}</td>
                          <td className="px-6 py-4 text-sm text-gray-900">{formatFileSize(photo.fileSize)}</td>
                          <td className="px-6 py-4 text-sm text-gray-900">{formatDateTime(photo.createdAt)}</td>
                          <td className="px-6 py-4">
                            {photo.selectedForPublishing ? (
                              <span className="badge-success">Selected</span>
                            ) : (
                              <span className="badge">Pending</span>
                            )}
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="flex items-center justify-end gap-2">
                              {photoUrls.get(photo.id) && (
                                <button
                                  onClick={(e) => { e.stopPropagation(); setSelectedPhotoForView(photo); }}
                                  className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                                  title="View"
                                >
                                  <Eye className="h-5 w-5" />
                                </button>
                              )}
                              <button
                                onClick={(e) => { e.stopPropagation(); togglePhoto(photo.id); }}
                                className={cn(
                                  'p-2 rounded-lg transition-colors',
                                  selectedPhotos.has(photo.id)
                                    ? 'text-indigo-600 hover:bg-indigo-100'
                                    : 'text-gray-500 hover:bg-gray-100'
                                )}
                                title={selectedPhotos.has(photo.id) ? 'Deselect' : 'Select'}
                              >
                                {selectedPhotos.has(photo.id) ? <CheckSquare className="h-5 w-5" /> : <Square className="h-5 w-5" />}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>

            {totalPages > 1 && (
              <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                <p className="text-sm text-gray-600">
                  Page {page + 1} of {totalPages} ({photosData?.totalElements} photos)
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary text-sm"
                  >
                    <ChevronLeft className="h-4 w-4 mr-1" />
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page === totalPages - 1}
                    className="btn-secondary text-sm"
                  >
                    Next
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </button>
                </div>
              </div>
            )}

      {selectedPhotoForView && photoUrls.get(selectedPhotoForView.id) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90" onClick={() => setSelectedPhotoForView(null)}>
          <div className="relative max-w-4xl max-h-[90vh]">
            <button
              onClick={() => setSelectedPhotoForView(null)}
              className="absolute -top-12 right-0 text-white hover:text-gray-300 z-10"
            >
              <X className="h-8 w-8" />
            </button>
            <img src={photoUrls.get(selectedPhotoForView.id)!} alt={selectedPhotoForView.originalFilename} className="max-w-full max-h-[90vh] object-contain" />
            <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent text-white">
              <p className="font-medium">{selectedPhotoForView.originalFilename}</p>
              <p className="text-sm opacity-75">{formatFileSize(selectedPhotoForView.fileSize)} • {formatDateTime(selectedPhotoForView.createdAt)}</p>
            </div>
          </div>
        </div>
      )}

      {selectedPhotoForView && photoUrls.get(selectedPhotoForView.id) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90" onClick={() => setSelectedPhotoForView(null)}>
          <div className="relative max-w-4xl max-h-[90vh]">
            <button
              onClick={() => setSelectedPhotoForView(null)}
              className="absolute -top-12 right-0 text-white hover:text-gray-300 z-10"
            >
              <X className="h-8 w-8" />
            </button>
            <img src={photoUrls.get(selectedPhotoForView.id)!} alt={selectedPhotoForView.originalFilename} className="max-w-full max-h-[90vh] object-contain" />
            <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent text-white">
              <p className="font-medium">{selectedPhotoForView.originalFilename}</p>
              <p className="text-sm opacity-75">{formatFileSize(selectedPhotoForView.fileSize)} • {formatDateTime(selectedPhotoForView.createdAt)}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}