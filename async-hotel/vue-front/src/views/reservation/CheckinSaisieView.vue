<template>
  <div class="content-wrapper">
    <h1>
      <router-link to="/reservation/liste">
        <i class="fa fa-angle-left"></i>
      </router-link>
      Enregistrement de Livraison/Récupération
    </h1>

    <div v-if="loading" class="alert alert-info">Chargement...</div>
    <div v-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-if="erreurPaiement" class="alert alert-warning">
      <strong>Attention !</strong> Le reste à payer de cette réservation n'est pas nul. Veuillez régulariser avant de procéder à la livraison.
    </div>

    <div v-if="!loading && !error && lignes.length > 0" class="card">
      <div class="card-title">Formulaire de livraison</div>

      <table class="table table-bordered">
        <thead>
          <tr>
            <th>Référence Produit</th>
            <th>Produit</th>
            <th>Image</th>
            <th>Date Livraison/Récupération</th>
            <th>Heure Livraison/Récupération</th>
            <th>Client</th>
            <th>Quantité Besoin</th>
            <th>Quantité</th>
            <th>Lieu Stockage</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(ligne, index) in lignes" :key="index">
            <td>{{ ligne.referenceproduit }}</td>
            <td>{{ ligne.produitlib }}</td>
            <td>
              <img v-if="ligne.image" :src="ligne.image" alt="img" style="max-width: 50px; max-height: 50px;" />
            </td>
            <td>
              <input type="date" v-model="ligne.daty" class="form-control" />
            </td>
            <td>
              <input type="time" v-model="ligne.heure" class="form-control" />
            </td>
            <td>{{ ligne.idclientlib }}</td>
            <td>{{ ligne.qteUtil }}</td>
            <td>
              <input type="number" v-model.number="ligne.qte" class="form-control" min="1" />
            </td>
            <td>
              <select v-model="ligne.magasin" class="form-control">
                <option value="">-- Choisir --</option>
                <option v-for="mag in magasins" :key="mag.id" :value="mag.id">
                  {{ mag.val }}
                </option>
              </select>
            </td>
            <td>
              <button class="btn btn-danger btn-sm" @click="supprimerLigne(index)">
                <i class="fa fa-times"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="actions">
        <button class="btn btn-primary" @click="enregistrerLivraison" :disabled="submitting">
          {{ submitting ? 'Enregistrement en cours...' : 'Enregistrer et Valider' }}
        </button>
        <button class="btn btn-secondary" @click="retour">Annuler</button>
      </div>

      <div v-if="message" class="result" :class="messageClass">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script setup>
function supprimerLigne(index) {
  lignes.value.splice(index, 1)
}
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLivraisonFormulaire, validerLivraison } from '@/services/services.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref(null)
const erreurPaiement = ref(false)
const lignes = ref([])
const magasins = ref([])
const submitting = ref(false)
const message = ref('')
const messageClass = ref('')

async function chargerFormulaire() {
  loading.value = true
  error.value = null
  try {
    const idresa = route.query.idresa
    if (!idresa) {
      error.value = "ID de réservation manquant"
      return
    }

    const data = await getLivraisonFormulaire(idresa)
    
    if (data.erreur) {
      erreurPaiement.value = true
      error.value = data.message || "Erreur lors du chargement"
      return
    }

    lignes.value = data.lignes.map(l => ({
      ...l,
      heure: new Date().toTimeString().substring(0, 5)
    }))

    // Charger les magasins (à adapter selon votre API)
    magasins.value = [
      { id: 'ATIPIK', val: 'ATIPIK' }
    ]

  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function enregistrerLivraison() {
  submitting.value = true
  message.value = ''
  try {
    const idresa = route.query.idresa
    
    // Valider les données
    for (let ligne of lignes.value) {
      if (!ligne.daty) {
        message.value = 'Veuillez renseigner la date de livraison pour tous les produits'
        messageClass.value = 'error'
        return
      }
      if (!ligne.magasin) {
        message.value = 'Veuillez sélectionner un lieu de stockage pour tous les produits'
        messageClass.value = 'error'
        return
      }
    }

    const result = await validerLivraison({
      idreservation: idresa,
      lignes: lignes.value
    })

    if (result.success) {
      message.value = 'Livraison enregistrée avec succès !'
      messageClass.value = 'success'
      setTimeout(() => {
        router.push('/reservation/fiche/' + idresa)
      }, 1500)
    } else {
      message.value = 'Erreur : ' + result.error
      messageClass.value = 'error'
    }
  } catch (e) {
    message.value = 'Erreur : ' + e.message
    messageClass.value = 'error'
  } finally {
    submitting.value = false
  }
}

function retour() {
  router.back()
}

onMounted(() => {
  chargerFormulaire()
})
</script>

<style scoped>
.content-wrapper {
  padding: 20px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.result {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
  text-align: center;
}

.result.success {
  background: #dff0d8;
  color: #3c763d;
}

.result.error {
  background: #f2dede;
  color: #a94442;
}

table {
  width: 100%;
  margin-top: 20px;
}

table td, table th {
  padding: 10px;
  text-align: left;
}

input, select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>