<template>
  <div class="home-page">
    <NavBar />
    <HeroSection />
    <StatsBar />
    <CultureSection />
    <JobSearchSection />
    <BenefitsSection />
    <GallerySection />
    <TestimonialsSection />
    <FaqSection />
    <CtaSection />
    <FooterSection />
  </div>
</template>

<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
import HeroSection from '@/components/HeroSection.vue'
import StatsBar from '@/components/StatsBar.vue'
import CultureSection from '@/components/CultureSection.vue'
import JobSearchSection from '@/views/JobSearchSection.vue'
import BenefitsSection from '@/components/BenefitsSection.vue'
import GallerySection from '@/components/GallerySection.vue'
import TestimonialsSection from '@/components/TestimonialsSection.vue'
import FaqSection from '@/components/FaqSection.vue'
import CtaSection from '@/components/CtaSection.vue'
import FooterSection from '@/components/FooterSection.vue'
</script>
