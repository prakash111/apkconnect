import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle, EmptyState } from '../../components/UI';
import * as adbApi from '../../api/adb';
import { AdbDevice } from '../../types';
import { colors, radius, spacing } from '../../theme/theme';

export default function AdbScreen({ navigation }: any) {
  const [devices, setDevices] = useState<AdbDevice[]>([]);
  const [host, setHost] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [busySerial, setBusySerial] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adbApi.listDevices();
      if (res.status === 'success') setDevices(res.devices || []);
      else setError(res.message || 'Could not list devices.');
    } catch (e: any) {
      setError(e?.message || 'Could not list devices.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onConnect = async () => {
    if (!host) {
      setError('Enter a device address, e.g. 192.168.1.20:5555');
      return;
    }
    setError('');
    setMessage('');
    try {
      const res = await adbApi.connectDevice(host);
      if (res.status === 'success') {
        setMessage(res.message || 'Connected.');
        setDevices(res.devices || []);
      } else {
        setError(res.message || 'Connect failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Connect failed.');
    }
  };

  const onDisconnect = async (serial: string) => {
    setBusySerial(serial);
    try {
      const res = await adbApi.disconnectDevice(serial);
      setDevices(res.devices || []);
    } finally {
      setBusySerial(null);
    }
  };

  const onInstall = async (serial: string) => {
    setBusySerial(serial);
    setError('');
    setMessage('');
    try {
      const res = await adbApi.installApk(serial, 'signed');
      if (res.status === 'success') setMessage(res.message || 'Installed.');
      else setError(res.message || 'Install failed.');
    } catch (e: any) {
      setError(e?.message || 'Install failed.');
    } finally {
      setBusySerial(null);
    }
  };

  return (
    <View style={styles.flex}>
      <FlatList
        data={devices}
        keyExtractor={item => item.serial}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListHeaderComponent={
          <View>
            <Card>
              <SectionTitle>Connect a device</SectionTitle>
              <Banner type="error" message={error} />
              <Banner type="success" message={message} />
              <Label>Device address (wireless ADB)</Label>
              <Input
                value={host}
                onChangeText={setHost}
                autoCapitalize="none"
                placeholder="192.168.1.20:5555"
              />
              <HelperText>
                The server connects to a device already reachable on its network via ADB over
                Wi-Fi. USB-attached devices on the server also appear below automatically.
              </HelperText>
              <Button title="Connect" onPress={onConnect} />
            </Card>
            <SectionTitle>Devices</SectionTitle>
          </View>
        }
        ListEmptyComponent={!loading ? <EmptyState title="No devices connected" /> : null}
        renderItem={({ item }) => (
          <View style={styles.deviceRow}>
            <View style={styles.deviceMain}>
              <Text style={styles.deviceSerial}>{item.serial}</Text>
              <Text style={styles.deviceState}>{item.state || 'device'}</Text>
            </View>
            <View style={styles.deviceActions}>
              <Button
                title="Install"
                small
                onPress={() => onInstall(item.serial)}
                loading={busySerial === item.serial}
              />
              <Button
                title="Logcat"
                small
                variant="secondary"
                onPress={() => navigation.navigate('Logcat', { serial: item.serial })}
              />
              <Button
                title="Disconnect"
                small
                variant="ghost"
                onPress={() => onDisconnect(item.serial)}
                loading={busySerial === item.serial}
              />
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  deviceRow: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: 8,
  },
  deviceMain: { marginBottom: spacing.sm },
  deviceSerial: { color: colors.text, fontWeight: '700', fontFamily: 'monospace' },
  deviceState: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  deviceActions: { flexDirection: 'row', gap: spacing.sm, flexWrap: 'wrap' },
});
