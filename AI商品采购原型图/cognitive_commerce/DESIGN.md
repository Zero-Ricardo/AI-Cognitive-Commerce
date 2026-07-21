---
name: Cognitive Commerce
colors:
  surface: '#f8f9ff'
  surface-dim: '#d0dbed'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dee9fc'
  surface-container-highest: '#d9e3f6'
  on-surface: '#121c2a'
  on-surface-variant: '#424754'
  inverse-surface: '#27313f'
  inverse-on-surface: '#eaf1ff'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#6b38d4'
  on-secondary: '#ffffff'
  secondary-container: '#8455ef'
  on-secondary-container: '#fffbff'
  tertiary: '#006947'
  on-tertiary: '#ffffff'
  tertiary-container: '#00855b'
  on-tertiary-container: '#f5fff6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#e9ddff'
  secondary-fixed-dim: '#d0bcff'
  on-secondary-fixed: '#23005c'
  on-secondary-fixed-variant: '#5516be'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f8f9ff'
  on-background: '#121c2a'
  surface-variant: '#d9e3f6'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-max: 1280px
  gutter: 24px
  margin-desktop: 48px
  margin-mobile: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

This design system is built for a next-generation e-commerce experience where artificial intelligence is a core utility, not just a feature. The brand personality is **intelligent, visionary, and hyper-efficient**. It seeks to evoke a sense of "magical precision"—the feeling that the interface understands the user's intent before they even articulate it.

The aesthetic follows a **Modern Corporate** foundation infused with **Glassmorphism** and **Tactile AI** accents. While the core shopping experience remains grounded and professional to ensure trust, AI-driven touchpoints (search, recommendations, assistants) utilize vibrant gradients and subtle outer glows to signal "active intelligence." The result is a clean, airy environment that feels both sophisticated and technologically advanced.

## Colors

The palette is designed to distinguish between human-generated content and machine-augmented insights. 

- **Primary & Secondary:** A vibrant transition from Blue to Purple is reserved exclusively for AI-enhanced features, such as match scores, assistant interactions, and smart search.
- **Neutrals:** We utilize a "Pure White" strategy for the primary canvas to maximize whitespace impact. Charcoal (#1F2937) provides high-contrast legibility for product details and specifications.
- **Semantic Colors:** Success Green and Warning Gold are used sparingly for transactional feedback (inventory status, price drops, or shipping alerts).
- **Surface Strategy:** Use soft grays (#F3F4F6) to define container boundaries without the visual weight of heavy borders.

## Typography

This design system relies on **Inter** to deliver a systematic and utilitarian feel. The hierarchy is established through significant weight shifts rather than excessive size variations. 

For display and headline levels, we use tighter letter spacing to maintain a modern, "tucked" appearance. Body text remains neutral with generous line heights to ensure readability during long browsing sessions. Label styles are set in medium or semi-bold weights to act as clear navigational anchors or metadata headers.

## Layout & Spacing

The layout utilizes a **12-column fluid grid** for desktop, transitioning to a **4-column grid** for mobile. 

- **Spaciousness:** We prioritize high whitespace to reduce cognitive load. Vertical stacking follows an 8px base unit, with standard gaps between sections set at 64px or 80px to create distinct visual "chapters."
- **Safe Zones:** Content is contained within a 1280px max-width wrapper on desktop to prevent excessive line lengths. 
- **Reflow:** On tablet and mobile, product grids should reflow from 4 columns to 2, ensuring product imagery remains the focal point. AI Assistant overlays should dock to the bottom of the screen on mobile devices.

## Elevation & Depth

Visual hierarchy is achieved through **Tonal Layers** and **Ambient Shadows**.

- **Level 0 (Base):** Pure white background.
- **Level 1 (Cards):** Soft gray (#F3F4F6) surfaces with no shadow, or white surfaces with a very diffuse, 4% opacity neutral shadow (0px 4px 20px).
- **Level 2 (Modals/Overlays):** White surfaces with a 10% opacity shadow (0px 10px 30px) and a subtle 1px border (#E5E7EB).
- **AI Elevation:** Elements powered by AI (like the Chat Assistant or Smart Match badges) use a **backlight effect**. This is achieved by adding a soft, low-opacity purple or blue outer glow (blur: 15px) instead of a standard shadow, making the component appear as if it is emitting light.

## Shapes

The design system uses a **Rounded** shape language to appear approachable and modern.

- **Standard Components:** Buttons, input fields, and small cards use a **12px (0.75rem)** radius.
- **Large Components:** Large product containers, hero sections, and the AI Assistant panel use a **24px (1.5rem)** radius to emphasize the "friendly tech" aesthetic.
- **Interactive Elements:** Filter chips and "AI Match" badges use a fully rounded (pill) shape to distinguish them from structural UI components.

## Components

### AI-Enhanced Search
The primary search bar features a **1px gradient border** (Blue to Purple). When focused, it emits a subtle 4px outer glow. The placeholder text should suggest intelligent queries (e.g., "Find a laptop for 4K video editing...").

### Product Cards
Cards are clean with minimal borders. A **"Match Score"** badge is positioned in the top-right corner, utilizing the AI gradient background with white text. On hover, the card should lift slightly (Elevation Level 2) and show a "Quick Match" summary.

### AI Assistant Chat
The chat trigger is a floating action button (FAB) with the AI gradient. Chat bubbles for the AI have a soft purple tint (#F5F3FF), while user bubbles are neutral charcoal (#1F2937).

### Buttons
- **Primary:** Solid Charcoal (#1F2937) with white text for standard actions (Add to Cart).
- **AI Action:** Solid AI Gradient for smart actions (Personalize, Compare with AI).
- **Secondary:** Transparent with a 1px gray border (#D1D5DB).

### Chips & Filters
Filter chips use a pill shape. Active states use the AI gradient if the filter is "Smart/AI-suggested," otherwise they toggle to a solid charcoal state.

### Input Fields
Inputs use the 12px rounded corner rule with a subtle #F3F4F6 background. Focus states should always be high-contrast or use the AI gradient if the input is part of a "Smart" flow.