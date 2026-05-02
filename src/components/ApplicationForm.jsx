import { useForm } from 'react-hook-form'
import { X, Loader2 } from 'lucide-react'
import { useMutation, useQueryClient } from 'react-query'
import toast from 'react-hot-toast'
import api from '../services/api'

export default function ApplicationForm({ application, onClose }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: application || {
      status: 'APPLIED',
      appliedDate: new Date().toISOString().split('T')[0]
    }
  })
  const queryClient = useQueryClient()

  const mutation = useMutation(
    (data) => application 
      ? api.put(`/applications/${application.id}`, data)
      : api.post('/applications', data),
    {
      onSuccess: () => {
        queryClient.invalidateQueries('applications')
        queryClient.invalidateQueries('stats')
        toast.success(application ? 'Application updated' : 'Application created')
        onClose()
      },
      onError: (err) => {
        toast.error(err.response?.data?.message || 'Something went wrong')
      }
    }
  )

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold">
            {application ? 'Edit Application' : 'New Application'}
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Company *</label>
              <input {...register('companyName', { required: true })} className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Job Title *</label>
              <input {...register('jobTitle', { required: true })} className="input-field" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Job Description</label>
            <textarea {...register('jobDescription')} rows={4} className="input-field" placeholder="Paste job description for AI analysis..." />
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Location</label>
              <input {...register('location')} className="input-field" placeholder="City, Remote..." />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Salary Range</label>
              <input {...register('salaryRange')} className="input-field" placeholder="$80k - $100k" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Status *</label>
              <select {...register('status')} className="input-field">
                <option value="APPLIED">Applied</option>
                <option value="SCREENING">Screening</option>
                <option value="INTERVIEW">Interview</option>
                <option value="OFFER">Offer</option>
                <option value="REJECTED">Rejected</option>
                <option value="WITHDRAWN">Withdrawn</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Applied Date *</label>
              <input type="date" {...register('appliedDate', { required: true })} className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Application URL</label>
              <input {...register('applicationUrl')} className="input-field" placeholder="https://..." />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Notes</label>
            <textarea {...register('notes')} rows={2} className="input-field" placeholder="Interview dates, contacts, etc." />
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={mutation.isLoading} className="btn-primary flex items-center gap-2">
              {mutation.isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              {application ? 'Update' : 'Create'} Application
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}