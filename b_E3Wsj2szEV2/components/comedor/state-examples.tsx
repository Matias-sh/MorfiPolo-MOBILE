"use client"

import { Loader2, WifiOff, AlertCircle, Info, CheckCircle, AlertTriangle, UtensilsCrossed } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

// Loading State Component
export function LoadingState({ message = "Cargando..." }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16">
      <Loader2 className="w-10 h-10 text-primary animate-spin" />
      <p className="mt-4 text-sm text-muted-foreground">{message}</p>
    </div>
  )
}

// Empty State Component
export function EmptyState({ 
  title = "Sin datos", 
  description = "No hay información disponible",
  icon: Icon = UtensilsCrossed 
}: { 
  title?: string
  description?: string
  icon?: React.ElementType
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16">
      <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-4">
        <Icon className="w-10 h-10 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold text-foreground">{title}</h3>
      <p className="mt-1 text-sm text-muted-foreground text-center px-8">
        {description}
      </p>
    </div>
  )
}

// Error Banner Component
export function ErrorBanner({ 
  message = "Error al cargar los datos",
  description,
  onRetry 
}: { 
  message?: string
  description?: string
  onRetry?: () => void 
}) {
  return (
    <div className="flex items-center gap-3 p-4 bg-destructive/10 border border-destructive/20 rounded-2xl">
      <AlertCircle className="w-5 h-5 text-destructive flex-shrink-0" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground">{message}</p>
        {description && (
          <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
        )}
      </div>
      {onRetry && (
        <Button 
          variant="ghost" 
          size="sm"
          onClick={onRetry}
          className="text-destructive hover:text-destructive hover:bg-destructive/10 flex-shrink-0"
        >
          Reintentar
        </Button>
      )}
    </div>
  )
}

// Offline Banner Component
export function OfflineBanner() {
  return (
    <div className="flex items-center gap-3 p-4 bg-warning/10 border border-warning/20 rounded-2xl">
      <WifiOff className="w-5 h-5 text-warning-foreground flex-shrink-0" />
      <div>
        <p className="text-sm font-medium text-foreground">Sin conexión</p>
        <p className="text-xs text-muted-foreground">Mostrando datos guardados</p>
      </div>
    </div>
  )
}

// Info Banner Component
export function InfoBanner({ 
  message,
  variant = "info" 
}: { 
  message: string
  variant?: "info" | "success" | "warning"
}) {
  const variants = {
    info: {
      bg: "bg-info/10",
      border: "border-info/20",
      icon: Info,
      iconColor: "text-info",
    },
    success: {
      bg: "bg-success/10",
      border: "border-success/20",
      icon: CheckCircle,
      iconColor: "text-success",
    },
    warning: {
      bg: "bg-warning/10",
      border: "border-warning/20",
      icon: AlertTriangle,
      iconColor: "text-warning-foreground",
    },
  }

  const v = variants[variant]
  const Icon = v.icon

  return (
    <div className={cn("flex items-start gap-3 p-4 rounded-2xl border", v.bg, v.border)}>
      <Icon className={cn("w-5 h-5 flex-shrink-0 mt-0.5", v.iconColor)} />
      <p className="text-sm text-foreground leading-relaxed">{message}</p>
    </div>
  )
}

// Skeleton Loader for Cards
export function CardSkeleton() {
  return (
    <div className="bg-card rounded-2xl border border-border p-4 animate-pulse">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1">
          <div className="h-4 bg-muted rounded-lg w-24" />
          <div className="mt-3 h-4 bg-muted rounded-lg w-full" />
          <div className="mt-2 h-4 bg-muted rounded-lg w-3/4" />
          <div className="mt-3 h-3 bg-muted rounded-lg w-32" />
        </div>
        <div className="w-5 h-5 bg-muted rounded" />
      </div>
    </div>
  )
}

// Multiple Card Skeletons
export function CardSkeletonList({ count = 3 }: { count?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: count }).map((_, i) => (
        <CardSkeleton key={i} />
      ))}
    </div>
  )
}
