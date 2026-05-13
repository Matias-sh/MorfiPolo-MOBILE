"use client"

import { Button } from "@/components/ui/button"
import { Check, X, Plus, ArrowRight } from "lucide-react"

export function DesignSystem() {
  return (
    <div className="p-6 bg-background min-h-screen">
      <h1 className="text-2xl font-bold text-foreground mb-8">Design System - Comedor App</h1>
      
      {/* Color Palette */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Paleta de Colores</h2>
        <div className="grid grid-cols-2 gap-3">
          <ColorSwatch name="Primary" className="bg-primary text-primary-foreground" />
          <ColorSwatch name="Secondary" className="bg-secondary text-secondary-foreground" />
          <ColorSwatch name="Success" className="bg-success text-success-foreground" />
          <ColorSwatch name="Warning" className="bg-warning text-warning-foreground" />
          <ColorSwatch name="Destructive" className="bg-destructive text-destructive-foreground" />
          <ColorSwatch name="Info" className="bg-info text-info-foreground" />
          <ColorSwatch name="Muted" className="bg-muted text-muted-foreground" />
          <ColorSwatch name="Accent" className="bg-accent text-accent-foreground" />
        </div>
        
        <h3 className="text-sm font-medium text-muted-foreground mt-6 mb-3">Superficies</h3>
        <div className="grid grid-cols-2 gap-3">
          <ColorSwatch name="Background" className="bg-background text-foreground border" />
          <ColorSwatch name="Card" className="bg-card text-card-foreground border" />
          <ColorSwatch name="Input" className="bg-input text-foreground border" />
          <ColorSwatch name="Border" className="bg-border text-foreground" />
        </div>
      </section>

      {/* Typography */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Tipografía</h2>
        <div className="space-y-4 bg-card p-4 rounded-2xl border border-border">
          <div>
            <p className="text-xs text-muted-foreground mb-1">Título Principal (24px/Bold)</p>
            <p className="text-2xl font-bold text-foreground">Menú del Día</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">Título Sección (18px/Semibold)</p>
            <p className="text-lg font-semibold text-foreground">Opciones Disponibles</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">Cuerpo (14px/Regular)</p>
            <p className="text-sm text-foreground">Muslo deshuesado con papas, calabaza y cebollas</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">Caption (12px/Medium)</p>
            <p className="text-xs font-medium text-muted-foreground">Horario para elegir: 08:00 - 11:00</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">Overline (10px/Medium/Uppercase)</p>
            <p className="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">Configuración</p>
          </div>
        </div>
      </section>

      {/* Buttons */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Botones</h2>
        
        <h3 className="text-sm font-medium text-muted-foreground mb-3">Primarios</h3>
        <div className="flex flex-wrap gap-3 mb-6">
          <Button className="bg-primary hover:bg-primary/90 text-primary-foreground rounded-2xl h-12 px-6">
            Ingresar
          </Button>
          <Button className="bg-primary hover:bg-primary/90 text-primary-foreground rounded-2xl h-12 px-6" disabled>
            Deshabilitado
          </Button>
        </div>

        <h3 className="text-sm font-medium text-muted-foreground mb-3">Con iconos</h3>
        <div className="flex flex-wrap gap-3 mb-6">
          <Button className="bg-primary hover:bg-primary/90 text-primary-foreground rounded-xl">
            <Check className="w-4 h-4 mr-2" />
            Elegir opción
          </Button>
          <Button variant="destructive" className="rounded-xl">
            <X className="w-4 h-4 mr-2" />
            Quitar
          </Button>
        </div>

        <h3 className="text-sm font-medium text-muted-foreground mb-3">Secundarios y Ghost</h3>
        <div className="flex flex-wrap gap-3">
          <Button variant="outline" className="rounded-xl">
            Cancelar
          </Button>
          <Button variant="ghost" className="rounded-xl text-primary">
            Ver más
            <ArrowRight className="w-4 h-4 ml-1" />
          </Button>
        </div>
      </section>

      {/* Badges */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Badges</h2>
        <div className="flex flex-wrap gap-3">
          <span className="inline-flex items-center px-3 py-1.5 bg-success/10 text-success rounded-full text-xs font-medium">
            <Check className="w-3.5 h-3.5 mr-1" />
            Abierto
          </span>
          <span className="inline-flex items-center px-3 py-1.5 bg-muted text-muted-foreground rounded-full text-xs font-medium">
            Cerrado
          </span>
          <span className="inline-flex items-center px-3 py-1.5 bg-primary text-primary-foreground rounded-full text-xs font-medium">
            <Check className="w-3.5 h-3.5 mr-1" />
            Ya elegiste
          </span>
          <span className="inline-flex items-center px-3 py-1.5 bg-warning/10 text-warning-foreground rounded-full text-xs font-medium">
            Pendiente
          </span>
        </div>
      </section>

      {/* Cards */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Cards</h2>
        
        <div className="space-y-4">
          {/* Menu Card */}
          <div className="bg-card rounded-2xl border border-border p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-base font-semibold text-foreground">28/04/2026</span>
              <span className="inline-flex items-center px-2 py-0.5 bg-muted rounded-full text-[10px] font-medium text-muted-foreground">
                Cerrado
              </span>
            </div>
            <p className="text-sm text-foreground">Muslo deshuesado con papas, calabaza y cebollas</p>
            <p className="mt-2 text-xs font-medium text-primary flex items-center gap-1">
              <Check className="w-3.5 h-3.5" />
              Ya seleccionaste esta opción
            </p>
          </div>

          {/* Selected Option Card */}
          <div className="bg-accent border-2 border-primary rounded-xl p-4">
            <p className="font-medium text-foreground">Milanesa de pollo con ensalada mixta</p>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-sm font-medium text-primary flex items-center gap-1.5">
                <Check className="w-4 h-4" />
                Seleccionado
              </span>
              <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive hover:bg-destructive/10">
                <X className="w-4 h-4 mr-1" />
                Quitar
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Spacing */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Espaciado Base</h2>
        <div className="flex items-end gap-4">
          <SpacingBlock size={4} />
          <SpacingBlock size={8} />
          <SpacingBlock size={12} />
          <SpacingBlock size={16} />
          <SpacingBlock size={24} />
          <SpacingBlock size={32} />
        </div>
      </section>

      {/* Border Radius */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Radios de Borde</h2>
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-primary rounded-lg flex items-center justify-center text-primary-foreground text-xs">8px</div>
          <div className="w-16 h-16 bg-primary rounded-xl flex items-center justify-center text-primary-foreground text-xs">12px</div>
          <div className="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center text-primary-foreground text-xs">16px</div>
          <div className="w-16 h-16 bg-primary rounded-3xl flex items-center justify-center text-primary-foreground text-xs">24px</div>
          <div className="w-16 h-16 bg-primary rounded-full flex items-center justify-center text-primary-foreground text-xs">full</div>
        </div>
      </section>

      {/* Touch Targets */}
      <section className="mb-12">
        <h2 className="text-lg font-semibold text-foreground mb-4">Touch Targets (mín. 48dp)</h2>
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-primary rounded-xl flex items-center justify-center">
            <Plus className="w-5 h-5 text-primary-foreground" />
          </div>
          <p className="text-sm text-muted-foreground">48x48dp mínimo para elementos interactivos</p>
        </div>
      </section>
    </div>
  )
}

function ColorSwatch({ name, className }: { name: string; className: string }) {
  return (
    <div className={`p-4 rounded-xl ${className}`}>
      <p className="text-sm font-medium">{name}</p>
    </div>
  )
}

function SpacingBlock({ size }: { size: number }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div 
        className="bg-primary" 
        style={{ width: size, height: size }}
      />
      <span className="text-xs text-muted-foreground">{size}</span>
    </div>
  )
}
