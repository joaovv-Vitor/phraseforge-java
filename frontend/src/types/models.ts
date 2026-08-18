export interface Author {
  id: number
  name: string
  slug: string
  birthYear: number | null
  deathYear: number | null
  biography: string | null
  phraseCount: number
  createdAt: string
  updatedAt: string
}

export interface AuthorSummary {
  id: number
  name: string
  slug: string
  birthYear: number | null
  deathYear: number | null
  phraseCount: number
}

export interface Category {
  id: number
  name: string
  slug: string
  description: string | null
  phraseCount: number
}

export interface CategorySummary {
  id: number
  name: string
  slug: string
  phraseCount: number
}

export interface Tag {
  id: number
  name: string
}

export interface AuthorRef {
  id: number
  name: string
  slug: string
}

export interface CategoryRef {
  id: number
  name: string
  slug: string
}

export interface TagRef {
  id: number
  name: string
}

export interface PhraseSummary {
  id: number
  content: string
  year: number | null
  language: string
  source: string | null
  author: AuthorRef
  categories: CategoryRef[]
  tags: TagRef[]
  createdAt: string
  favorited: boolean
}

export interface Phrase extends PhraseSummary {
  updatedAt: string
}

export interface Paged<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PhraseFilters {
  query?: string
  authorId?: number
  categoryId?: number
  tagId?: number
  language?: string
  page?: number
  size?: number
}

export interface AuthorPayload {
  name: string
  birthYear: number | null
  deathYear: number | null
  biography: string | null
}

export interface CategoryPayload {
  name: string
  description: string | null
}

export interface TagPayload {
  name: string
}

export interface PhrasePayload {
  content: string
  authorId: number
  year: number | null
  language: string
  source: string | null
  categoryIds: number[]
  tagIds: number[]
}
