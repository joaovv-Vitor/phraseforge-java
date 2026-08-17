export default function ErrorState({ message }: { message: string }) {
  return (
    <div className="py-16 text-center">
      <p className="mb-1 font-serif text-lg text-ink">Algo deu errado</p>
      <p className="text-sm text-ink-faint">{message}</p>
    </div>
  )
}
