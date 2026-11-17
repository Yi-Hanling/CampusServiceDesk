import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const role = ref(Number(localStorage.getItem('role')) || -1)
    const realName = ref(localStorage.getItem('realName') || '')

    function setUserInfo(data) {
        token.value = data.token
        role.value = data.role
        realName.value = data.realName

        localStorage.setItem('token', data.token)
        localStorage.setItem('role', data.role)
        localStorage.setItem('realName', data.realName)
    }

    function logout() {
        token.value = ''
        role.value = -1
        realName.value = ''
        localStorage.clear()
    }

    return { token, role, realName, setUserInfo, logout }
})