<template>
  <div class="content-wrapper">
    <h1 class="page-title">Produits disponibles à la location</h1>
    
    <div class="card">
      <form @submit.prevent="chargerProduits" class="filter-form">
        <div class="form-row">
          <div class="form-group">
            <label for="dateDebut">Date début :</label>
            <input 
              type="date" 
              id="dateDebut" 
              v-model="dateDebut" 
              class="form-control" 
              required
              @change="calculerDateFin"
            />
          </div>
          
          <div class="form-group">
            <label for="nbJours">Nombre de jours :</label>
            <input 
              type="number" 
              id="nbJours" 
              v-model.number="nbJours" 
              class="form-control" 
              min="1"
              required
              @input="calculerDateFin"
            />
          </div>

          <div class="form-group">
            <label for="dateFin">Date fin (calculée) :</label>
            <input 
              type="date" 
              id="dateFin" 
              v-model="dateFin" 
              class="form-control" 
              readonly
            />
          </div>
          
          <div class="form-group">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Chargement...' : 'Rechercher' }}
            </button>
          </div>
        </div>
      </form>
    </div>

    <div v-if="error" class="alert alert-danger">{{ error }}</div>
    
    <div v-if="!loading && produits.length === 0 && searched" class="alert alert-info">
      Aucun produit disponible pour cette période.
    </div>

    <div v-if="produits.length > 0" class="card">
      <div class="results-header">
        <h2 class="section-title">{{ total }} produit(s) disponible(s)</h2>
        <p>Du {{ dateDebut }} au {{ dateFin }} ({{ nbJours }} jour(s))</p>
      </div>
      
      <table class="styled-table">
        <thead>
          <tr>
            <th>Image</th>
            <th>Référence</th>
            <th>Libellé</th>
            <th>Catégorie</th>
            <th>Unité</th>
            <th>Prix unitaire</th>
            <th>Quantité disponible</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="produit in produits" :key="produit.id">
            <td>
              <img 
                v-if="produit.image" 
                :src="`/asynclocation/${produit.image}`" 
                :alt="produit.libelle"
                class="product-image"
              />
              <span v-else class="no-image">Pas d'image</span>
            </td>
            <td>{{ produit.reference }}</td>
            <td>{{ produit.libelle }}</td>
            <td>{{ produit.categorie }}</td>
            <td>{{ produit.unite }}</td>
            <td>{{ formatPrice(produit.pu) }}</td>
            <td class="text-center">
              <span class="badge-quantity">{{ produit.qteDisponible }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getProduitsDisponibles } from '@/services/services.js'

const dateDebut = ref('')
const nbJours = ref(1)
const dateFin = ref('')
const produits = ref([])
const loading = ref(false)
const error = ref(null)
const searched = ref(false)
const total = ref(0)

function calculerDateFin() {
  if (!dateDebut.value || !nbJours.value || nbJours.value < 1) {
    dateFin.value = ''
    return
  }
  
  const debut = new Date(dateDebut.value)
  debut.setDate(debut.getDate() + nbJours.value)
  dateFin.value = debut.toISOString().split('T')[0]
}

function formatPrice(price) {
  if (!price) return '-'
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(price) + ' Ar'
}

async function chargerProduits() {
  if (!dateDebut.value || !nbJours.value || nbJours.value < 1) {
    error.value = 'Veuillez renseigner la date de début et le nombre de jours'
    return
  }

  calculerDateFin()

  loading.value = true
  error.value = null
  searched.value = true
  
  try {
    const response = await getProduitsDisponibles(dateDebut.value, dateFin.value)
    produits.value = response.data || []
    total.value = response.total || 0
  } catch (e) {
    error.value = e.message || 'Erreur lors du chargement des produits'
    produits.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.page-title {
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 1.5rem;
  color: #2c3e50;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.filter-form .form-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}

.form-group {
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #34495e;
}

.form-control {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.btn {
  padding: 0.6rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary {
  background: #3498db;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2980b9;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.alert {
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.alert-info {
  background: #d9edf7;
  color: #31708f;
}

.alert-danger {
  background: #f2dede;
  color: #a94442;
}

.results-header {
  margin-bottom: 1rem;
}

.section-title {
  font-size: 1.3rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #34495e;
}

.styled-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.styled-table th,
.styled-table td {
  border: 1px solid #eee;
  padding: 0.75rem;
  text-align: left;
}

.styled-table th {
  background: #f8f8f8;
  font-weight: 600;
}

.styled-table tbody tr:hover {
  background: #f9f9f9;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.no-image {
  display: inline-block;
  width: 60px;
  height: 60px;
  line-height: 60px;
  text-align: center;
  background: #eee;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #999;
}

.text-center {
  text-align: center;
}

.badge-quantity {
  background: #27ae60;
  color: white;
  padding: 0.3rem 0.6rem;
  border-radius: 12px;
  font-weight: 600;
}

input[readonly] {
  background-color: #f5f5f5;
  cursor: not-allowed;
}
</style>