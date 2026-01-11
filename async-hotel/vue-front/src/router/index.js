import { createRouter, createWebHistory } from 'vue-router'

import ProformaSaisieView from '@/views/proforma/ProformaSaisieView.vue'
import ProformaListeView from '@/views/proforma/ProformaListeView.vue'
import ProformaFicheView from '@/views/proforma/ProformaFicheView.vue'
import ReservationListeView from '@/views/reservation/ReservationListeView.vue'
import ReservationFicheView from '@/views/reservation/ReservationFicheView.vue'
import MvtCaisseCautionSaisieView from '@/views/caisse/MvtCaisseCautionSaisieView.vue'


const routes = [
  {
    path: '/',
    redirect: '/proforma/liste'
  },
  {
    path: '/proforma/saisie',
    component: ProformaSaisieView
  },
  {
    path: '/proforma/liste',
    component: ProformaListeView
  },
  {
    path: '/proforma/fiche/:id',
    component: ProformaFicheView,
    name: 'ProformaFicheView'
  },

  {
    path: '/reservation/liste',
    component: ReservationListeView
  },

  {
    path: '/reservation/fiche/:id',
    component: ReservationFicheView
  },

  {
    path: '/caisse/mvt-caisse-saisie-caution',
    component: MvtCaisseCautionSaisieView,
    name: 'MvtCaisseCautionSaisieView'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router