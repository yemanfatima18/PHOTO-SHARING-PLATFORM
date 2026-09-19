import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, X, Camera } from 'lucide-react';
import { galleryApi } from '../../api/endpoints';
import { PublicGalleryPhoto } from '../../types';

export function PublicGalleryPhotosPage() {
  const { token } = useParams<{ token: string }>();
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [photoUrls, setPhotoUrls] = useState<Map<string, string>>(new Map());

  const accessToken = sessionStorage.getItem(`gallery_token_${token}`);

  const { data: photos, isLoading, error } = useQuery({
    queryKey: ['public-gallery-photos', token],
    queryFn: () => galleryApi.getPublicPhotos(accessToken!),
    enabled: !!accessToken && !!token,
    retry: false,
  });

  // Fetch photo URLs for display
  useEffect(() => {
    if (photos && accessToken) {
      const newUrls = new Map<string, string>();
      photos.forEach(photo => {
        const url = `/api/public/galleries/${token}/photos/${photo.id}`;
        newUrls.set(photo.id, url);
      });
      setPhotoUrls(newUrls);
    }
  }, [photos, accessToken, token]);

  useEffect(() => {
    if (!accessToken && !isLoading) {
      window.location.href = `/gallery/${token}`;
    }
  }, [accessToken, isLoading, token]);

  const handleKeyDown = (e: KeyboardEvent) => {
    if (!lightboxOpen) return;
    if (e.key === 'Escape') setLightboxOpen(false);
    if (e.key === 'ArrowLeft' && selectedIndex !== null && selectedIndex > 0) setSelectedIndex(selectedIndex - 1);
    if (e.key === 'ArrowRight' && selectedIndex !== null && photos && selectedIndex < photos.length - 1) setSelectedIndex(selectedIndex + 1);
  };

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [lightboxOpen, selectedIndex, photos]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (error || !photos) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-900">
        <div className="text-center text-white">
          <Camera className="h-16 w-16 text-gray-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold mb-2">Gallery Access Expired</h2>
          <p className="text-gray-400 mb-6">Your session has expired. Please re-enter the PIN.</p>
          <a href={`/gallery/${token}`} className="btn-primary inline-block">
            Enter PIN Again
          </a>
        </div>
      </div>
    );
  }

  if (photos.length === 0) {
    return (
      <div className="text-center py-20 text-gray-400">
        <Camera className="h-16 w-16 mx-auto mb-4 text-gray-600" />
        <h3 className="text-lg font-medium">No photos in this gallery</h3>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900">
      <header className="fixed top-0 left-0 right-0 z-40 bg-black/80 backdrop-blur-sm border-b border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-4">
              <span className="text-white font-medium">Gallery</span>
              <span className="px-2 py-0.5 text-xs bg-indigo-600 text-white rounded-full">{photos.length} photos</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-400 hidden sm:block">
                Press ESC to close, arrows to navigate
              </span>
            </div>
          </div>
        </div>
      </header>

      <main className="pt-16 pb-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
{photos.map((photo: PublicGalleryPhoto, index: number) => (
              <button
                key={photo.id}
                onClick={() => { setSelectedIndex(index); setLightboxOpen(true); }}
                className="relative aspect-[4/3] rounded-lg overflow-hidden bg-gray-800 hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {photoUrls.get(photo.id) && (
                  <img
                    src={photoUrls.get(photo.id)!}
                    alt={photo.originalFilename}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                )}
                <div className="absolute bottom-0 left-0 right-0 p-2 bg-gradient-to-t from-black/70 to-transparent">
                  <p className="text-white text-xs truncate">{photo.originalFilename}</p>
                </div>
              </button>
            ))}
          </div>
        </div>
      </main>

      {selectedIndex !== null && photos[selectedIndex] && photoUrls.get(photos[selectedIndex].id) && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/95"
          onClick={() => { setLightboxOpen(false); setSelectedIndex(null); }}
        >
          <button
            onClick={(e) => { e.stopPropagation(); setLightboxOpen(false); setSelectedIndex(null); }}
            className="absolute top-6 right-6 p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors"
            aria-label="Close"
          >
            <X className="h-8 w-8" />
          </button>

          {selectedIndex !== null && selectedIndex > 0 && (
            <button
              onClick={(e) => { e.stopPropagation(); setSelectedIndex(selectedIndex! - 1); }}
              className="absolute left-6 p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors"
              aria-label="Previous"
            >
              <ChevronLeft className="h-8 w-8" />
            </button>
          )}

          <div className="relative max-w-5xl max-h-[85vh] px-4">
            <img
              src={photoUrls.get(photos[selectedIndex].id)!}
              alt={photos[selectedIndex].originalFilename}
              className="max-w-full max-h-[85vh] object-contain mx-auto"
            />
            <div className="absolute bottom-8 left-0 right-0 text-center text-white">
              <p className="font-medium mb-1">{photos[selectedIndex].originalFilename}</p>
              <p className="text-sm text-gray-400">Photo {selectedIndex! + 1} of {photos.length}</p>
            </div>
          </div>

          {selectedIndex !== null && photos && selectedIndex < photos.length - 1 && (
            <button
              onClick={(e) => { e.stopPropagation(); setSelectedIndex(selectedIndex! + 1); }}
              className="absolute right-6 p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors"
              aria-label="Next"
            >
              <ChevronRight className="h-8 w-8" />
            </button>
          )}
        </div>
      )}
    </div>
  );
}