import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Image, ChevronLeft, Lock, Unlock, Copy, ExternalLink, X, AlertCircle, Check } from 'lucide-react';
import { galleryApi, photoApi } from '../../api/endpoints';
import { Gallery, Photo } from '../../types';
import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const gallerySchema = z.object({
  pin: z.string().length(6, 'PIN must be exactly 6 digits').regex(/^\d{6}$/, 'PIN must be 6 digits'),
});

type GalleryForm = z.infer<typeof gallerySchema>;

export function AdminEventGalleryPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showPinModal, setShowPinModal] = useState(false);
  const [gallery, setGallery] = useState<Gallery | null>(null);
  const [selectedPhotos, setSelectedPhotos] = useState<Photo[]>([]);
  const [copied, setCopied] = useState(false);
  const [publishConfirm, setPublishConfirm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GalleryForm>({
    resolver: zodResolver(gallerySchema),
  });

  const { data: galleryData, isLoading: galleryLoading } = useQuery({
    queryKey: ['gallery', eventId],
    queryFn: () => galleryApi.getGallery(eventId!),
    enabled: !!eventId,
    retry: false,
  });

  const { data: selectedPhotosData } = useQuery({
    queryKey: ['selected-photos', eventId],
    queryFn: () => photoApi.getSelectedPhotos(eventId!),
    enabled: !!eventId,
  });

  const createGalleryMutation = useMutation({
    mutationFn: (data: GalleryForm) => galleryApi.create(eventId!, data),
    onSuccess: (newGallery) => {
      toast.success('Gallery created');
      setGallery(newGallery);
      setShowPinModal(false);
      reset();
      queryClient.invalidateQueries({ queryKey: ['gallery', eventId] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to create gallery');
    },
  });

  const publishMutation = useMutation({
    mutationFn: () => galleryApi.publish(eventId!),
    onSuccess: (publishedGallery) => {
      toast.success('Gallery published successfully!');
      setGallery(publishedGallery);
      queryClient.invalidateQueries({ queryKey: ['gallery', eventId] });
      setPublishConfirm(false);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to publish gallery');
      setPublishConfirm(false);
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: () => galleryApi.unpublish(eventId!),
    onSuccess: (unpublishedGallery) => {
      toast.success('Gallery unpublished');
      setGallery(unpublishedGallery);
      queryClient.invalidateQueries({ queryKey: ['gallery', eventId] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to unpublish gallery');
    },
  });

  const handleCreateGallery = (data: GalleryForm) => {
    createGalleryMutation.mutate(data);
  };

  const handlePublish = () => {
    setPublishConfirm(true);
  };

  const confirmPublish = () => {
    publishMutation.mutate();
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  useEffect(() => {
    if (galleryData) {
      setGallery(galleryData);
    }
  }, [galleryData]);

  useEffect(() => {
    if (selectedPhotosData) {
      setSelectedPhotos(selectedPhotosData);
    }
  }, [selectedPhotosData]);

  if (galleryLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent"></div>
        </div>
      </div>
    );
  }

  const hasSelectedPhotos = selectedPhotos.length > 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link to={`/events/${eventId}`} className="p-2 rounded-lg text-gray-500 hover:bg-gray-100">
            <ChevronLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Gallery Management</h1>
            <p className="text-gray-600">Create and publish galleries for your event</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Selected Photos</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{selectedPhotos.length}</p>
            </div>
            <div className="p-3 bg-indigo-100 rounded-xl">
              <Image className="h-8 w-8 text-indigo-600" />
            </div>
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Gallery Status</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {gallery?.published ? 'Published' : gallery ? 'Draft' : 'Not created'}
              </p>
            </div>
            <div className="p-3 bg-green-100 rounded-xl">
              {gallery?.published ? <Unlock className="h-8 w-8 text-green-600" /> : <Lock className="h-8 w-8 text-yellow-600" />}
            </div>
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Photos in Gallery</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{gallery?.photoCount || 0}</p>
            </div>
            <div className="p-3 bg-purple-100 rounded-xl">
              <Image className="h-8 w-8 text-purple-600" />
            </div>
          </div>
        </div>
      </div>

      {!gallery ? (
        <div className="card">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Create Gallery</h2>
          </div>
          <div className="p-6">
            {!hasSelectedPhotos ? (
              <div className="text-center py-8">
                <AlertCircle className="h-16 w-16 text-yellow-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No photos selected</h3>
                <p className="text-gray-500 mb-6">Select photos from the Photos tab to create a gallery</p>
                <Link to={`/events/${eventId}/photos`} className="btn-primary">
                  <Image className="h-5 w-5 mr-2" />
                  Go to Photos
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit(handleCreateGallery)} className="max-w-md space-y-4">
                <div>
                  <label htmlFor="pin" className="label">Gallery PIN (6 digits)</label>
                  <input
                    id="pin"
                    type="password"
                    {...register('pin')}
                    className="input text-center tracking-widest text-2xl"
                    placeholder="••••••"
                    maxLength={6}
                    disabled={createGalleryMutation.isPending}
                  />
                  {errors.pin && <p className="text-sm text-red-600 mt-1">{errors.pin.message}</p>}
                </div>
                <p className="text-sm text-gray-500">
                  This PIN will be required for customers to access the gallery.
                </p>
                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => navigate(`/events/${eventId}/photos`)}
                    className="btn-secondary flex-1"
                  >
                    Back to Photos
                  </button>
                  <button
                    type="submit"
                    disabled={createGalleryMutation.isPending}
                    className="btn-primary flex-1"
                  >
                    {createGalleryMutation.isPending ? 'Creating...' : 'Create Gallery'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      ) : (
        <>
          {gallery && !gallery.published && (
            <div className="card mb-6">
              <div className="p-6 border-b border-gray-200">
                <h2 className="text-lg font-semibold text-gray-900">Gallery Draft</h2>
              </div>
              <div className="p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">Status</p>
                    <p className="font-medium text-yellow-600">Draft - Not visible to public</p>
                  </div>
                  <span className="badge-warning">Draft</span>
                </div>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">PIN</p>
                    <p className="font-mono text-lg tracking-widest text-gray-900">••••••</p>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">Photos included</p>
                    <p className="font-medium text-gray-900">{gallery.photoCount} photos</p>
                  </div>
                </div>
                <div className="flex gap-3 pt-4">
                  <button
                    onClick={handlePublish}
                    disabled={publishMutation.isPending}
                    className="btn-primary flex-1"
                  >
                    {publishMutation.isPending ? 'Publishing...' : 'Publish Gallery'}
                  </button>
                  <button
                    onClick={() => unpublishMutation.mutate()}
                    disabled={unpublishMutation.isPending}
                    className="btn-danger flex-1"
                  >
                    {unpublishMutation.isPending ? 'Unpublishing...' : 'Delete Gallery'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {gallery && gallery.published && (
            <div className="card mb-6">
              <div className="p-6 border-b border-gray-200 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-gray-900">Published Gallery</h2>
                <span className="badge-success">Live</span>
              </div>
              <div className="p-6 space-y-4">
                <div>
                  <p className="text-sm text-gray-500">Shareable URL</p>
                  <div className="flex gap-2 mt-1">
                    <input
                      type="text"
                      value={gallery.shareUrl || ''}
                      readOnly
                      className="input flex-1 bg-gray-50"
                    />
                    <button
                      onClick={() => copyToClipboard(gallery.shareUrl || '')}
                      className="btn-secondary flex items-center gap-1"
                    >
                      {copied ? <Check className="h-5 w-5" /> : <Copy className="h-5 w-5" />}
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                    <a
                      href={gallery.shareUrl || '#'}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn-secondary flex items-center gap-1"
                    >
                      <ExternalLink className="h-5 w-5" />
                      Open
                    </a>
                  </div>
                </div>
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div>
                    <p className="text-sm text-gray-500">PIN</p>
                    <p className="font-mono text-lg tracking-widest text-gray-900">••••••</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm text-gray-500">Photos:</p>
                    <p className="font-medium text-gray-900">{gallery.photoCount}</p>
                  </div>
                </div>
                <div className="flex gap-3 pt-4">
                  <button
                    onClick={() => unpublishMutation.mutate()}
                    disabled={unpublishMutation.isPending}
                    className="btn-danger flex-1"
                  >
                    {unpublishMutation.isPending ? 'Unpublishing...' : 'Unpublish Gallery'}
                  </button>
                </div>
              </div>
            </div>
          )}

          <div className="card">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">Gallery Preview</h2>
            </div>
            <div className="p-6">
              {selectedPhotos.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <Image className="h-12 w-12 text-gray-300 mx-auto mb-2" />
                  <p>No photos in gallery</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                  {selectedPhotos.map((photo: Photo) => (
                    <div key={photo.id} className="relative aspect-[4/3] rounded-lg overflow-hidden bg-gray-100">
                      {photo.signedUrl && (
                        <img
                          src={photo.signedUrl}
                          alt={photo.originalFilename}
                          className="w-full h-full object-cover"
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {publishConfirm && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50" onClick={() => setPublishConfirm(false)} />
            <div className="relative bg-white rounded-xl shadow-xl max-w-md w-full p-6">
              <div className="text-center">
                <Unlock className="h-16 w-16 text-indigo-600 mx-auto mb-4" />
                <h2 className="text-xl font-semibold text-gray-900 mb-2">Publish Gallery?</h2>
                <p className="text-gray-600 mb-6">
                  This will make the gallery publicly accessible via the shareable link.
                  Customers will need to enter the PIN to view photos.
                </p>
                <div className="flex gap-3 justify-center">
                  <button
                    onClick={() => setPublishConfirm(false)}
                    className="btn-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={confirmPublish}
                    disabled={publishMutation.isPending}
                    className="btn-primary"
                  >
                    {publishMutation.isPending ? 'Publishing...' : 'Yes, Publish'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {showPinModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50" onClick={() => setShowPinModal(false)} />
            <div className="relative bg-white rounded-xl shadow-xl max-w-md w-full p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-gray-900">Create Gallery</h2>
                <button
                  onClick={() => setShowPinModal(false)}
                  className="p-1 rounded-lg text-gray-500 hover:bg-gray-100"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleSubmit(handleCreateGallery)} className="space-y-4">
                <div>
                  <label htmlFor="pin" className="label">Gallery PIN (6 digits)</label>
                  <input
                    id="pin"
                    type="password"
                    {...register('pin')}
                    className="input text-center tracking-widest text-2xl"
                    placeholder="••••••"
                    maxLength={6}
                    disabled={createGalleryMutation.isPending}
                  />
                  {errors.pin && <p className="text-sm text-red-600 mt-1">{errors.pin.message}</p>}
                </div>
                <p className="text-sm text-gray-500">
                  This PIN will be required for customers to access the gallery.
                </p>
                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowPinModal(false)}
                    className="btn-secondary flex-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={createGalleryMutation.isPending}
                    className="btn-primary flex-1"
                  >
                    {createGalleryMutation.isPending ? 'Creating...' : 'Create Gallery'}
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