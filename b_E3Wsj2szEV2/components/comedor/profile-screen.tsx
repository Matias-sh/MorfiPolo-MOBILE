"use client"

import { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { KeyRound, LogOut, Building2, Shield, ChevronRight, Sparkles, User } from "lucide-react"
import { cn } from "@/lib/utils"

interface ProfileScreenProps {
  userName?: string
  userRole?: string
  userDni?: string
  onChangePassword: () => void
  onLogout: () => void
}

export function ProfileScreen({
  userName = "Matias Federico Britez",
  userRole = "Usuario",
  userDni = "12345678",
  onChangePassword,
  onLogout,
}: ProfileScreenProps) {
  const [mounted, setMounted] = useState(false)
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const initials = userName.split(" ").map(n => n[0]).join("").slice(0, 2).toUpperCase()

  return (
    <div className="relative flex h-full flex-col overflow-hidden bg-background">
      {/* Hero Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={mounted ? { opacity: 1 } : {}}
        className="relative overflow-hidden"
      >
        {/* Animated Gradient Background */}
        <motion.div 
          className="absolute inset-0 bg-gradient-to-br from-primary via-primary/95 to-accent/80"
          animate={{
            backgroundPosition: ["0% 0%", "100% 100%", "0% 0%"],
          }}
          transition={{ duration: 12, repeat: Infinity, ease: "linear" }}
          style={{ backgroundSize: "200% 200%" }}
        />
        
        {/* Floating decorative elements */}
        <motion.div
          className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-white/10"
          animate={{ 
            scale: [1, 1.2, 1],
            rotate: [0, 45, 0]
          }}
          transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="absolute -bottom-20 -left-16 h-40 w-40 rounded-full bg-white/5"
          animate={{ 
            scale: [1, 1.15, 1],
            x: [0, 20, 0]
          }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut", delay: 1 }}
        />

        <div className="relative px-6 pb-16 pt-8">
          {/* User Avatar and Info */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={mounted ? { opacity: 1, y: 0 } : {}}
            transition={{ delay: 0.2 }}
            className="flex flex-col items-center text-center"
          >
            {/* Avatar with animated ring */}
            <motion.div
              className="relative mb-4"
              whileHover={{ scale: 1.05 }}
            >
              {/* Animated outer ring */}
              <motion.div
                className="absolute -inset-2 rounded-full"
                style={{
                  background: "conic-gradient(from 0deg, transparent, white, transparent)",
                }}
                animate={{ rotate: 360 }}
                transition={{ duration: 4, repeat: Infinity, ease: "linear" }}
              />
              
              {/* Avatar container */}
              <div className="relative flex h-24 w-24 items-center justify-center rounded-full bg-white/20 backdrop-blur-sm">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-white shadow-lg">
                  <span className="text-2xl font-bold text-primary">{initials}</span>
                </div>
              </div>

              {/* Verified badge */}
              <motion.div
                className="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full bg-success shadow-lg"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.5, type: "spring" }}
              >
                <Shield className="h-4 w-4 text-white" />
              </motion.div>
            </motion.div>

            {/* Name and role */}
            <motion.h1
              className="text-xl font-bold text-white"
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.4 }}
            >
              {userName}
            </motion.h1>
            
            <motion.div
              className="mt-2 flex items-center gap-2"
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.5 }}
            >
              <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-medium text-white/90 backdrop-blur-sm">
                {userRole}
              </span>
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs text-white/70">
                DNI: {userDni}
              </span>
            </motion.div>
          </motion.div>
        </div>

        {/* Wave decoration */}
        <svg
          className="absolute -bottom-1 left-0 w-full"
          viewBox="0 0 1440 100"
          fill="none"
          preserveAspectRatio="none"
        >
          <motion.path
            d="M0 100V50C180 80 360 20 540 35C720 50 900 80 1080 60C1260 40 1350 20 1440 40V100H0Z"
            className="fill-background"
            initial={{ d: "M0 100V70C180 70 360 70 540 70C720 70 900 70 1080 70C1260 70 1350 70 1440 70V100H0Z" }}
            animate={{ d: "M0 100V50C180 80 360 20 540 35C720 50 900 80 1080 60C1260 40 1350 20 1440 40V100H0Z" }}
            transition={{ duration: 0.8, delay: 0.3 }}
          />
        </svg>
      </motion.div>

      {/* Content */}
      <div className="flex-1 overflow-auto px-5 pb-28 -mt-4">
        {/* Settings Section */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={mounted ? { opacity: 1, y: 0 } : {}}
          transition={{ delay: 0.6 }}
          className="mb-6"
        >
          <h3 className="mb-3 px-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Configuracion
          </h3>
          
          <div className="overflow-hidden rounded-3xl bg-card shadow-premium">
            {/* Change Password */}
            <motion.button
              onClick={onChangePassword}
              whileHover={{ backgroundColor: "var(--muted)" }}
              whileTap={{ scale: 0.99 }}
              className="group flex w-full items-center gap-4 p-4 text-left transition-all"
            >
              <motion.div
                className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/10 to-accent/10"
                whileHover={{ scale: 1.1, rotate: 5 }}
              >
                <KeyRound className="h-6 w-6 text-primary" />
              </motion.div>
              <div className="flex-1">
                <p className="font-semibold text-foreground">Cambiar contrasena</p>
                <p className="mt-0.5 text-xs text-muted-foreground">Actualiza tu contrasena de acceso</p>
              </div>
              <motion.div
                className="flex h-8 w-8 items-center justify-center rounded-full bg-muted/50 text-muted-foreground"
                whileHover={{ x: 4, backgroundColor: "var(--primary)", color: "white" }}
              >
                <ChevronRight className="h-4 w-4" />
              </motion.div>
            </motion.button>

            <div className="mx-4 h-px bg-border" />

            {/* Logout */}
            <motion.button
              onClick={() => setShowLogoutConfirm(true)}
              whileHover={{ backgroundColor: "var(--destructive)/5" }}
              whileTap={{ scale: 0.99 }}
              className="group flex w-full items-center gap-4 p-4 text-left transition-all"
            >
              <motion.div
                className="flex h-12 w-12 items-center justify-center rounded-2xl bg-destructive/10"
                whileHover={{ scale: 1.1, rotate: -5 }}
              >
                <LogOut className="h-6 w-6 text-destructive" />
              </motion.div>
              <div className="flex-1">
                <p className="font-semibold text-destructive">Cerrar sesion</p>
                <p className="mt-0.5 text-xs text-muted-foreground">Salir de tu cuenta</p>
              </div>
              <motion.div
                className="flex h-8 w-8 items-center justify-center rounded-full bg-destructive/10 text-destructive"
                whileHover={{ x: 4 }}
              >
                <ChevronRight className="h-4 w-4" />
              </motion.div>
            </motion.button>
          </div>
        </motion.div>

        {/* About Section */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={mounted ? { opacity: 1, y: 0 } : {}}
          transition={{ delay: 0.8 }}
          className="flex flex-col items-center pt-6 text-center"
        >
          {/* Logo */}
          <motion.div
            className="relative mb-4"
            whileHover={{ scale: 1.05 }}
          >
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-muted to-muted/50 shadow-sm">
              <Building2 className="h-8 w-8 text-muted-foreground" />
            </div>
            <motion.div
              className="absolute -right-1 -top-1"
              animate={{ rotate: [0, 15, -15, 0], scale: [1, 1.1, 1] }}
              transition={{ duration: 3, repeat: Infinity, repeatDelay: 2 }}
            >
              <Sparkles className="h-4 w-4 text-primary/50" />
            </motion.div>
          </motion.div>
          
          <p className="text-xs text-muted-foreground">Desarrollado por</p>
          <p className="mt-1 text-sm font-bold text-foreground">Direccion de Ingenieria</p>
          
          <motion.div 
            className="mt-4 flex items-center gap-3"
            initial={{ opacity: 0 }}
            animate={mounted ? { opacity: 1 } : {}}
            transition={{ delay: 1 }}
          >
            <div className="rounded-xl bg-muted/50 px-4 py-2">
              <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                Gobierno
              </p>
            </div>
            <motion.div 
              className="h-1.5 w-1.5 rounded-full bg-primary/30"
              animate={{ scale: [1, 1.3, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            />
            <div className="rounded-xl bg-muted/50 px-4 py-2">
              <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                Formosa
              </p>
            </div>
          </motion.div>

          <motion.p
            className="mt-6 text-[10px] text-muted-foreground/60"
            initial={{ opacity: 0 }}
            animate={mounted ? { opacity: 1 } : {}}
            transition={{ delay: 1.2 }}
          >
            Version 2.0.0
          </motion.p>
        </motion.div>
      </div>

      {/* Logout Confirmation Modal */}
      {showLogoutConfirm && (
        <motion.div
          className="absolute inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          onClick={() => setShowLogoutConfirm(false)}
        >
          <motion.div
            className="mx-6 w-full max-w-sm overflow-hidden rounded-3xl bg-card shadow-2xl"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 text-center">
              <motion.div
                className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", delay: 0.1 }}
              >
                <LogOut className="h-8 w-8 text-destructive" />
              </motion.div>
              
              <h3 className="text-lg font-bold text-foreground">Cerrar sesion?</h3>
              <p className="mt-2 text-sm text-muted-foreground">
                Tendras que volver a ingresar tus credenciales
              </p>
            </div>

            <div className="flex gap-3 border-t border-border p-4">
              <motion.button
                onClick={() => setShowLogoutConfirm(false)}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="flex-1 rounded-2xl bg-muted py-3 text-sm font-semibold text-foreground"
              >
                Cancelar
              </motion.button>
              <motion.button
                onClick={() => {
                  setShowLogoutConfirm(false)
                  onLogout()
                }}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="flex-1 rounded-2xl bg-destructive py-3 text-sm font-semibold text-white shadow-lg shadow-destructive/25"
              >
                Salir
              </motion.button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  )
}
