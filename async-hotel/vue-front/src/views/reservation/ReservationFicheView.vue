<template>
  <div class="content-wrapper">
    <h1>
      <router-link to="/reservation/liste">
        <i class="fa fa-angle-left"></i>
      </router-link>
      Fiche de la réservation
    </h1>

    <div v-if="loading">Chargement...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else>
      <div class="box-fiche">
        <div class="box">
          <div class="box-body">
            <div><strong>Id</strong> : {{ fiche.id }}</div>
            <div><strong>Client</strong> : {{ fiche.client }}</div>
            <div><strong>Date</strong> : {{ fiche.date }}</div>
            <div><strong>Remarque</strong> : {{ fiche.remarque }}</div>
            <div><strong>État</strong> : {{ fiche.etat }}</div>
            <div><strong>Montant Total</strong> : {{ fiche.montantTotal?.toLocaleString() }} Ar</div>
            <div><strong>Montant Payé</strong> : {{ fiche.paye?.toLocaleString() }} Ar</div>
            <div><strong>Reste à Payer</strong> : {{ fiche.resteAPayer?.toLocaleString() }} Ar</div>
            
            <!-- Lien vers la facture/vente -->
            <div v-if="fiche.idVente">
              <strong>Facture :</strong>
              <router-link :to="'/vente/fiche/' + fiche.idVente" class="btn btn-link">
                Voir la facture {{ fiche.idVente }}
              </router-link>
            </div>
          </div>
          <div class="box-footer">
            <button v-if="fiche.idVente" class="btn btn-secondary" @click="allerFacture">
              Voir la facture
            </button>
            <button class="btn btn-warning" @click="imprimer">Imprimer</button>
            <button class="btn btn-primary" @click="livraisonRecuperation">
              Livraison/Récupération
            </button>
            <button class="btn btn-success" @click="retour">
              Retour
            </button>
          </div>
        </div>
      </div>

      <!-- Onglets -->
      <div class="nav-tabs-custom">
        <ul class="nav nav-tabs">
          <li :class="{ active: tab === 'details' }">
            <a @click="tab = 'details'">Détail(s)</a>
          </li>
          <li :class="{ active: tab === 'cautions' }">
            <a @click="tab = 'cautions'">Caution(s)</a>
          </li>
        </ul>
        <div class="tab-content">
          <div v-if="tab === 'details'">
            <table class="table table-bordered" v-if="fiche.details && fiche.details.length > 0">
              <thead>
                <tr>
                  <th>Produit</th>
                  <th>Désignation</th>
                  <th>Quantité (Article)</th>
                  <th>Quantité (En Jour)</th>
                  <th>Date De Début</th>
                  <th>Prix Unitaire</th>
                  <th>Montant</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in fiche.details" :key="d.id">
                  <td>{{ d.produit }}</td>
                  <td>{{ d.designation }}</td>
                  <td>{{ d.qteArticle }}</td>
                  <td>{{ d.qteJour }}</td>
                  <td>{{ d.dateDebut }}</td>
                  <td>{{ d.pu?.toLocaleString() }} Ar</td>
                  <td>{{ d.montant?.toLocaleString() }} Ar</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="tab === 'cautions'">
            <table class="table table-bordered" v-if="fiche.cautions && fiche.cautions.length > 0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Montant</th>
                  <th>% Appliqué</th>
                  <th>Date</th>
                  <th>Débit</th>
                  <th>Crédit</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in fiche.cautions" :key="c.id">
                  <td>{{ c.id }}</td>
                  <td>{{ c.montant?.toLocaleString() }} Ar</td>
                  <td>{{ c.pctApplique }} %</td>
                  <td>{{ c.daty }}</td>
                  <td>{{ c.debit?.toLocaleString() }} Ar</td>
                  <td>{{ c.credit?.toLocaleString() }} Ar</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchReservationFiche } from '@/services/services.js'

const route = useRoute()
const router = useRouter()

const fiche = ref({})
const loading = ref(true)
const error = ref(null)
const tab = ref('details')

async function loadFiche() {
  try {
    const data = await fetchReservationFiche(route.params.id)
    if (data.success === false) {
      error.value = data.error
    } else {
      fiche.value = data
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function allerFacture() {
  router.push('/vente/fiche/' + fiche.value.idVente)
}

function imprimer() {
  window.open(`/asynclocation/ExportPDF?action=proforma&id=${fiche.value.id}`, '_blank')
}

function livraisonRecuperation() {
  router.push({
    path: '/reservation/checkin-saisie',
    query: { idresa: fiche.value.id }
  })
}

function retour() {
  router.push({
    path: '/reservation/checkout-saisie',
    query: { idresa: fiche.value.id }
  })
}

onMounted(() => {
  loadFiche()
})
</script>

<style scoped>
.content-wrapper {
  padding: 2rem;
}
.box-fiche {
  margin-bottom: 2rem;
}
.nav-tabs-custom {
  margin-top: 2rem;
}
</style>