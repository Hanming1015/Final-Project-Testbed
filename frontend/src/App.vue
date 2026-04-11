<template>
  <div>{{ info }}</div>
  <router-view/>
</template>

<script>
import $ from 'jquery';
import { ref } from 'vue';


export default {
  name: 'App',
  setup: () => {
    // You can use Composition API here if needed
    let info = ref('');

    $.ajax({
      url: 'http://localhost:3000/api/hello',
      method: 'GET',
      success: function(data) {
        info.value = data;
      },
      error: function(error) {
        console.error('Error fetching info:', error);
      }
    });

    return {
      info
    };
  }
}
</script>

<style>
</style>
