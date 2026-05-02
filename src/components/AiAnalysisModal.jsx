import { useState } from 'react'
import { useQuery } from 'react-query'
import { X, Brain, Target, AlertCircle, Lightbulb, Loader2 } from 'lucide-react'
import api from '../services/api'

export default function AiAnalysisModal({ jobApplication, onClose }) {
  const [selectedResumeId, setSelectedResumeId] = useState(null)

  const { data: resumes } = useQuery('resumes', () => 
    api.get('/resumes').then(r => r.data)
  )

  const { data: analysis, isLoading } = useQuery(
    ['analysis', selectedResumeId, jobApplication.id],
    () => api.post(`/resumes/${selectedResumeId}/analyze/${jobApplication.id}`).then(r => r.data),
    { enabled: !!selectedResumeId }
  )

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b flex justify-between items-center">
          <div className="flex items-center gap-2">
            <Brain className="w-5 h-5 text-purple-600" />
            <h2 className="text-xl font-bold">AI Resume Analysis</h2>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          <div className="bg-gray-50 p-4 rounded-lg">
            <p className="text-sm text-gray-500 mb-1">Analyzing for:</p>
            <p className="font-semibold">{jobApplication.jobTitle} at {jobApplication.companyName}</p>
          </div>

          {!selectedResumeId ? (
            <div>
              <label className="block text-sm font-medium mb-2">Select a resume to analyze:</label>
              <div className="space-y-2">
                {resumes?.map(resume => (
                  <button
                    key={resume.id}
                    onClick={() => setSelectedResumeId(resume.id)}
                    className="w-full text-left p-4 border rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors"
                  >
                    <p className="font-medium">{resume.originalFileName}</p>
                    <p className="text-sm text-gray-500">
                      Uploaded {new Date(resume.uploadedAt).toLocaleDateString()}
                    </p>
                  </button>
                ))}
                {(!resumes || resumes.length === 0) && (
                  <p className="text-center text-gray-500 py-4">No resumes available. Upload one first!</p>
                )}
              </div>
            </div>
          ) : isLoading ? (
            <div className="flex flex-col items-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-purple-600 mb-3" />
              <p className="text-gray-600">Analyzing your resume with AI...</p>
            </div>
          ) : analysis ? (
            <div className="space-y-6">
              <div className="flex items-center justify-center">
                <div className="relative w-32 h-32">
                  <svg className="w-full h-full transform -rotate-90">
                    <circle cx="64" cy="64" r="56" stroke="#E5E7EB" strokeWidth="12" fill="none" />
                    <circle 
                      cx="64" cy="64" r="56" 
                      stroke={analysis.matchPercentage >= 70 ? '#10B981' : analysis.matchPercentage >= 40 ? '#F59E0B' : '#EF4444'}
                      strokeWidth="12" 
                      fill="none"
                      strokeDasharray={`${analysis.matchPercentage * 3.52} 351.86`}
                    />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-3xl font-bold">{analysis.matchPercentage}%</span>
                  </div>
                </div>
              </div>
              <p className="text-center text-gray-600">
                {analysis.matchPercentage >= 70 ? 'Great match!' : 
                 analysis.matchPercentage >= 40 ? 'Moderate match. Room for improvement.' : 
                 'Low match. Consider updating your resume.'}
              </p>

              {analysis.missingSkills?.length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-3">
                    <AlertCircle className="w-5 h-5 text-red-600" />
                    <h3 className="font-semibold text-red-900">Missing Skills</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {analysis.missingSkills.map((skill, i) => (
                      <span key={i} className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm">
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <Lightbulb className="w-5 h-5 text-amber-600" />
                  <h3 className="font-semibold text-amber-900">AI Suggestions</h3>
                </div>
                <ul className="space-y-2">
                  {analysis.suggestions.map((suggestion, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-amber-800">
                      <Target className="w-4 h-4 mt-0.5 shrink-0" />
                      <span>{suggestion}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <button 
                onClick={() => setSelectedResumeId(null)}
                className="w-full btn-secondary"
              >
                Analyze with different resume
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}