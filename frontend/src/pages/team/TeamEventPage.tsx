import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useDropzone } from 'react-dropzone';
import { Link } from 'react-router-dom';
import { Image, Upload, X, ChevronLeft, RotateCcw, AlertCircle, CheckCircle } from 'lucide-react';
import { photoApi, eventApi } from '../../api/endpoints';
import { Photo } from '../../types';
import { formatFileSize, cn } from '../../utils/helpers';
import { useState, useCallback } from 'react';
import toast from 'react-hot-toast';

interface UploadFile {
  file: File;
  preview: string;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
  photoId?: string;
}

export function TeamEventPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const queryClient = useQueryClient();
  const [uploadFiles, setUploadFiles] = useState<UploadFile[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [activeTab, setActiveTab] = useState<'upload' | 'my-photos'>('upload');

  const { data: event, isLoading: eventLoading } = useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventApi.getById(eventId!),
    enabled: !!eventId,
  });

  const { data: photosData, isLoading: photosLoading, refetch: refetchPhotos } = useQuery({
    queryKey: ['my-photos', eventId],
    queryFn: () => photoApi.getMyPhotos(eventId!, 0, 50),
    enabled: !!eventId,
  });

  const uploadMutation = useMutation({
    mutationFn: ({ eventId, file }: { eventId: string; file: File }) => photoApi.upload(eventId, file),
    onSuccess: (data, variables) => {
      const index = uploadFiles.findIndex(f => f.file === variables.file);
      if (index !== -1) {
        setUploadFiles(prev => {
          const newFiles = [...prev];
          newFiles[index] = { ...newFiles[index], status: 'completed', photoId: data.id };
          return newFiles;
        });
        toast.success(`${variables.file.name} uploaded successfully`);
      }
    },
    onError: (error: any, variables) => {
      const index = uploadFiles.findIndex(f => f.file === variables.file);
      if (index !== -1) {
        setUploadFiles(prev => {
          const newFiles = [...prev];
          newFiles[index] = { ...newFiles[index], status: 'error', error: error.response?.data?.message || 'Upload failed' };
          return newFiles;
        });
        toast.error(`${variables.file.name}: ${error.response?.data?.message || 'Upload failed'}`);
      }
    },
  });

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const validFiles = acceptedFiles.filter(file => {
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        toast.error(`${file.name}: Invalid file type. Only JPEG, PNG, WebP allowed.`);
        return false;
      }
      if (file.size > 50 * 1024 * 1024) {
        toast.error(`${file.name}: File size exceeds 50MB limit.`);
        return false;
      }
      return true;
    });

    const newFiles: UploadFile[] = validFiles.map(file => ({
      file,
      preview: URL.createObjectURL(file),
      status: 'pending' as const,
    }));

    setUploadFiles(prev => [...prev, ...newFiles]);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png'],
      'image/webp': ['.webp'],
    },
    maxSize: 50 * 1024 * 1024,
    multiple: true,
    disabled: isUploading,
  });

  const removeFile = (index: number) => {
    setUploadFiles(prev => {
      const newFiles = [...prev];
      URL.revokeObjectURL(newFiles[index].preview);
      newFiles.splice(index, 1);
      return newFiles;
    });
  };

  const retryUpload = (index: number) => {
    const file = uploadFiles[index];
    if (!file || file.status !== 'error') return;

    setUploadFiles(prev => {
      const newFiles = [...prev];
      newFiles[index] = { ...newFiles[index], status: 'pending', error: undefined };
      return newFiles;
    });

    uploadMutation.mutate({ eventId: eventId!, file: file.file });
  };

  const startUpload = () => {
    const pendingFiles = uploadFiles.filter(f => f.status === 'pending' || f.status === 'error');
    if (pendingFiles.length === 0) return;

    setIsUploading(true);

    pendingFiles.forEach(file => {
      uploadMutation.mutate({ eventId: eventId!, file: file.file });
    });

    setIsUploading(false);
    refetchPhotos();
    queryClient.invalidateQueries({ queryKey: ['event', eventId] });
  };

  const clearCompleted = () => {
    setUploadFiles(prev => {
      prev.forEach(f => URL.revokeObjectURL(f.preview));
      return prev.filter(f => f.status !== 'completed');
    });
  };

  const photos = photosData?.content || [];

  if (eventLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent"></div>
        </div>
      </div>
    );
  }

  const pendingCount = uploadFiles.filter(f => f.status === 'pending' || f.status === 'error').length;
  const completedCount = uploadFiles.filter(f => f.status === 'completed').length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link to="/team/dashboard" className="p-2 rounded-lg text-gray-500 hover:bg-gray-100">
            <ChevronLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{event?.name || 'Event'}</h1>
            <p className="text-gray-600">{event?.description || 'No description'}</p>
          </div>
        </div>
      </div>

      <div className="mb-6">
        <div className="flex gap-4 border-b border-gray-200">
          <button
            onClick={() => setActiveTab('upload')}
            className={cn(
              'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
              activeTab === 'upload'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <Upload className="h-4 w-4 mr-2 inline" />
            Upload Photos
            {uploadFiles.length > 0 && (
              <span className="ml-2 px-2 py-0.5 text-xs bg-indigo-100 text-indigo-600 rounded-full">
                {uploadFiles.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('my-photos')}
            className={cn(
              'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
              activeTab === 'my-photos'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <Image className="h-4 w-4 mr-2 inline" />
            My Photos ({photos.length})
          </button>
        </div>
      </div>

      {activeTab === 'upload' && (
        <div className="card">
          <div className="p-6 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">Upload Photos</h2>
              <div className="flex items-center gap-4">
                {completedCount > 0 && (
                  <button onClick={clearCompleted} className="text-sm text-gray-500 hover:text-gray-700">
                    Clear completed
                  </button>
                )}
              </div>
            </div>
          </div>

          <div className="p-6">
            <div
              {...getRootProps()}
              className={cn(
                'border-2 border-dashed rounded-xl p-8 text-center transition-colors',
                isDragActive ? 'border-indigo-500 bg-indigo-50' : 'border-gray-300 hover:border-gray-400'
              )}
            >
              <input {...getInputProps()} />
              <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-lg font-medium text-gray-900 mb-2">
                {isDragActive ? 'Drop files here' : 'Drag & drop photos here, or click to select'}
              </p>
              <p className="text-sm text-gray-500 mb-4">
                JPEG, PNG, WebP up to 50MB each
              </p>
              <p className="text-xs text-gray-400">
                Multiple files supported
              </p>
            </div>

            {uploadFiles.length > 0 && (
              <div className="mt-6 space-y-3 max-h-96 overflow-y-auto">
                {uploadFiles.map((file, index) => (
                  <div key={index} className={cn(
                    'flex items-center gap-4 p-4 rounded-lg border',
                    file.status === 'completed' ? 'border-green-200 bg-green-50' :
                    file.status === 'error' ? 'border-red-200 bg-red-50' :
                    file.status === 'uploading' ? 'border-blue-200 bg-blue-50' :
                    'border-gray-200'
                  )}>
                    <div className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100">
                      <img src={file.preview} alt={file.file.name} className="w-full h-full object-cover" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-gray-900 truncate">{file.file.name}</p>
                      <p className="text-sm text-gray-500">{formatFileSize(file.file.size)}</p>
                      {file.error && <p className="text-sm text-red-600 mt-1">{file.error}</p>}
                    </div>
                    <div className="flex items-center gap-2">
                      {file.status === 'pending' && (
                        <span className="badge">Pending</span>
                      )}
                      {file.status === 'uploading' && (
                        <span className="badge-info flex items-center gap-1">
                          <span className="animate-spin rounded-full h-3 w-3 border-2 border-current border-t-transparent"></span>
                          Uploading
                        </span>
                      )}
                      {file.status === 'completed' && (
                        <span className="badge-success flex items-center gap-1">
                          <CheckCircle className="h-3 w-3" />
                          Done
                        </span>
                      )}
                      {file.status === 'error' && (
                        <span className="badge-danger flex items-center gap-1">
                          <AlertCircle className="h-3 w-3" />
                          Error
                        </span>
                      )}
                      {file.status === 'error' && (
                        <button
                          onClick={() => retryUpload(index)}
                          className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                          title="Retry"
                        >
                          <RotateCcw className="h-5 w-5" />
                        </button>
                      )}
                      {(file.status === 'pending' || file.status === 'error') && (
                        <button
                          onClick={() => removeFile(index)}
                          className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50"
                          title="Remove"
                        >
                          <X className="h-5 w-5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="mt-6 flex justify-end gap-3">
              {uploadFiles.some(f => f.status === 'pending' || f.status === 'error') && (
                <button
                  onClick={startUpload}
                  disabled={isUploading}
                  className="btn-primary flex items-center gap-2 px-6"
                >
                  {isUploading ? (
                    <>
                      <span className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></span>
                      Uploading...
                    </>
                  ) : (
                    <>
                      <Upload className="h-5 w-5" />
                      Start Upload ({pendingCount})
                    </>
                  )}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'my-photos' && (
        <div className="card">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">My Uploaded Photos</h2>
          </div>

          {photosLoading ? (
            <div className="p-12 text-center">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-indigo-600 border-t-transparent mx-auto"></div>
            </div>
          ) : photos.length === 0 ? (
            <div className="p-12 text-center">
              <Image className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No photos uploaded yet</h3>
              <p className="text-gray-500">Upload photos from the Upload tab</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 p-6">
                {photos.map((photo: Photo) => (
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
            </>
          )}
        </div>
      )}
    </div>
  );
}